/** Where a company's emails actually come from: its own sender, the platform default, or the server's MAIL_* variables. */
export type MailSource = 'COMPANY' | 'PLATFORM' | 'ENVIRONMENT';

/**
 * The editor for ONE scope - a client company, or the platform default (clientCompanyId null).
 * The password is NEVER part of this - the server only says whether one is saved (passwordSet).
 * The first block is the row saved for exactly this scope (null when none); the "effective" block is
 * what emails for this scope are actually sent with right now.
 */
export interface MailSettings {
  clientCompanyId: number | null;
  scopeName: string;
  hasStoredSettings: boolean;
  host: string | null;
  port: number | null;
  username: string | null;
  fromAddress: string | null;
  enabled: boolean | null;
  passwordSet: boolean;
  updatedAt: string | null;
  effectiveSource: MailSource;
  effectiveHost: string | null;
  effectivePort: number | null;
  effectiveUsername: string | null;
  effectiveFromAddress: string | null;
  usable: boolean;
  problem: string | null;
}

/** One line of the overview table. clientCompanyId null = the platform default row. */
export interface MailOverviewRow {
  clientCompanyId: number | null;
  companyName: string;
  hasOwnSettings: boolean;
  ownEnabled: boolean;
  effectiveSource: MailSource;
  effectiveFromAddress: string | null;
  usable: boolean;
  problem: string | null;
}

export interface MailOverview {
  platform: MailOverviewRow;
  companies: MailOverviewRow[];
}

/** password is write-only: null on an update means "keep the saved password". */
export interface MailSettingsRequest {
  host: string;
  port: number;
  username: string;
  fromAddress: string;
  password: string | null;
  enabled: boolean;
}

export interface MailTestResult {
  sent: boolean;
  message: string;
}
