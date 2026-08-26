import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

interface ActiveConfirm extends ConfirmRequest {
  resolve: (result: boolean) => void;
}

/**
 * Promise-based confirmation dialog, rendered once by ConfirmDialogComponent
 * near the app root. Usage: `const ok = await confirmDialog.ask({...})`.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly _active = signal<ActiveConfirm | null>(null);
  readonly active = this._active.asReadonly();

  ask(request: ConfirmRequest): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      this._active.set({ ...request, resolve });
    });
  }

  respond(result: boolean): void {
    const current = this._active();
    if (!current) return;
    this._active.set(null);
    current.resolve(result);
  }
}
