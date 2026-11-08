import { build } from 'esbuild';
import { copyFile, mkdir, rm, writeFile } from 'node:fs/promises';
import { dirname, isAbsolute, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '../..');
const output = resolve(root, 'build/logent-runtime');
const privacy = resolve(here, 'privacy-policy.ts');
await mkdir(resolve(output, 'THIRD_PARTY_LICENSES'), { recursive: true });
const result = await build({
  absWorkingDir: root,
  entryPoints: [resolve(here, 'cli.ts')],
  outfile: resolve(output, 'claude-mem.mjs'),
  bundle: true,
  platform: 'node',
  format: 'esm',
  target: 'node22.13',
  sourcemap: false,
  metafile: true,
  legalComments: 'inline',
  logLevel: 'silent',
  alias: {
    'bun:sqlite': resolve(here, 'node-sqlite.ts'),
    zod: resolve(here, 'node_modules/zod/index.js'),
  },
  plugins: [{
    name: 'logent-privacy-boundary',
    setup(plugin) {
      plugin.onResolve({ filter: /(?:logger|SettingsDefaultsManager|paths)\.js$/ }, args => {
        if ((args.path.endsWith('logger.js') || args.importer.endsWith('/utils/redaction.ts')) && /(?:logger|SettingsDefaultsManager|paths)\.js$/.test(args.path)) {
          return { path: privacy };
        }
      });
    },
  }],
});
await writeFile(resolve(output, 'metafile.json'), `${JSON.stringify(result.metafile, null, 2)}\n`);
await copyFile(resolve(here, 'node_modules/zod/LICENSE'), resolve(output, 'THIRD_PARTY_LICENSES/zod-LICENSE'));

const closure = resolve(output, 'source-closure');
await rm(closure, { recursive: true, force: true });
const inputs = new Set([
  ...Object.keys(result.metafile.inputs),
  'logent/runtime/package.json',
  'logent/runtime/package-lock.json',
  'logent/runtime/build.mjs',
  'LICENSE',
  'NOTICE',
  'UPSTREAM_COMMIT',
]);
for (const input of inputs) {
  const source = resolve(root, input);
  const name = relative(root, source);
  if (name.startsWith('..') || isAbsolute(name)) throw new Error('소스가 저장소 밖에 있어요.');
  const destination = resolve(closure, name);
  await mkdir(dirname(destination), { recursive: true });
  await copyFile(source, destination);
}