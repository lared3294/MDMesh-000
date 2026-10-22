const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { stripTypeScriptTypes } = require('node:module');
const source = fs.readFileSync(require('node:path').join(__dirname, '../../src/api/deviceApps.ts'), 'utf8');
const code = stripTypeScriptTypes(source.replace(/^import .*;$/gm, '').replace(/^export /gm, ''));
const api = {};
vm.runInNewContext(code + '\nObject.assign(exports, { latestAppChange, scanFromHistory });', { exports: api });
const change = (id, type, status, completedAt) => ({ id, type, status, completedAt });

test('queued, delivered, failed, unsupported and expired package commands do not refresh inventory', () => {
  for (const type of ['app.install', 'app.uninstall']) {
    for (const status of ['pending', 'delivered', 'failed', 'unsupported', 'expired']) {
      assert.equal(api.latestAppChange([change(1, type, status, 200)], 100), undefined);
    }
  }
});
test('both successful install and uninstall invalidate an older scan, but not a newer one', () => {
  for (const type of ['app.install', 'app.uninstall']) {
    const cmd = change(1, type, 'done', 200);
    assert.equal(api.latestAppChange([cmd], 100), cmd);
    assert.equal(api.latestAppChange([cmd], 200), undefined);
    assert.equal(api.latestAppChange([cmd], 300), undefined);
  }
});
test('completion order wins over command id, including changes while the panel was closed', () => {
  const slow = change(1, 'app.install', 'done', 400);
  const fast = change(2, 'app.uninstall', 'done', 200);
  assert.equal(api.latestAppChange([fast, slow], 300), slow);
  assert.equal(api.latestAppChange([change(3, 'apps.scan', 'done', 500)], 300), undefined);
});
test('a successful change without a saved inventory also requests a scan', () => {
  const cmd = change(1, 'app.install', 'done', 200);
  assert.equal(api.latestAppChange([cmd], 0), cmd);
});
test('only completed full inventories replace the installed-app list', () => {
  const full = { apps: [{ pkg: 'com.example.hidden', launchable: false }], includeAll: true };
  const history = [
    { id: 4, type: 'apps.scan', status: 'pending' },
    { id: 3, type: 'apps.scan', status: 'done', detail: '{broken' },
    { id: 2, type: 'apps.scan', status: 'done', detail: JSON.stringify({ apps: [] }) },
    { id: 1, type: 'apps.scan', status: 'done', detail: JSON.stringify(full), completedAt: 100 },
  ];
  const saved = api.scanFromHistory(history, true);
  assert.equal(saved.apps[0].pkg, 'com.example.hidden');
  assert.equal(saved.scannedAt, 100);
  assert.equal(api.scanFromHistory(history.slice(0, 3), true), null);
});
