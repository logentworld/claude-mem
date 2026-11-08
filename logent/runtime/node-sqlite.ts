import { createRequire } from 'node:module';
import type { DatabaseSync as SQLiteDatabase, SQLInputValue } from 'node:sqlite';

const require = createRequire(import.meta.url);

export class Database {
  private db: SQLiteDatabase;
  private depth = 0;

  constructor(path: string) {
    const { DatabaseSync } = require('node:sqlite') as { DatabaseSync: new (path: string) => SQLiteDatabase };
    this.db = new DatabaseSync(path);
    this.db.exec('PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 5000; PRAGMA secure_delete = ON; PRAGMA journal_mode = DELETE;');
  }

  prepare(sql: string) {
    return this.db.prepare(sql);
  }

  run(sql: string, ...parameters: SQLInputValue[]) {
    return parameters.length ? this.db.prepare(sql).run(...parameters) : this.db.exec(sql);
  }

  transaction<Args extends unknown[], Result>(fn: (...args: Args) => Result): (...args: Args) => Result {
    return (...args: Args) => {
      const name = `logent_${this.depth++}`;
      const nested = this.depth > 1;
      try {
        this.db.exec(nested ? `SAVEPOINT ${name}` : 'BEGIN IMMEDIATE');
        try {
          const result = fn(...args);
          if (result instanceof Promise) throw new Error('비동기 트랜잭션은 지원하지 않아요.');
          this.db.exec(nested ? `RELEASE ${name}` : 'COMMIT');
          return result;
        } catch (error) {
          this.db.exec(nested ? `ROLLBACK TO ${name}; RELEASE ${name}` : 'ROLLBACK');
          throw error;
        }
      } finally {
        this.depth--;
      }
    };
  }

  close() {
    this.db.close();
  }
}
