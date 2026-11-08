import { join } from 'node:path';
import { homedir } from 'node:os';

export const USER_SETTINGS_PATH = join(process.env.LOGENT_MEM_DATA_DIR ?? join(homedir(), '.logent/plugins/claude-mem/data'), 'privacy-policy.json');

export class SettingsDefaultsManager {
  static loadFromFile(_path: string) {
    return {
      CLAUDE_MEM_REDACT_ENABLED: 'true',
      CLAUDE_MEM_REDACT_GENERIC_ENABLED: 'true',
      CLAUDE_MEM_REDACT_DISABLED_BUILTINS: '',
      CLAUDE_MEM_REDACT_CUSTOM_PATTERNS: '[]',
      CLAUDE_MEM_REDACT_LOG_MATCHES: 'false',
    };
  }
}

function diagnostic(severity: 'warning' | 'error') {
  process.stderr.write(`${JSON.stringify({ severity, code: 'PRIVACY_DIAGNOSTIC' })}\n`);
}

export const logger = {
  warn: (..._details: unknown[]) => diagnostic('warning'),
  error: (..._details: unknown[]) => diagnostic('error'),
  debug: (..._details: unknown[]) => diagnostic('warning'),
};
