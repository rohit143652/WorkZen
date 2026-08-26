import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: number;
  kind: ToastKind;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<ToastMessage[]>([]);
  readonly toasts = this._toasts.asReadonly();
  private nextId = 1;

  success(text: string): void { this.push('success', text); }
  error(text: string): void { this.push('error', text); }
  info(text: string): void { this.push('info', text); }
  warning(text: string): void { this.push('warning', text); }

  dismiss(id: number): void {
    this._toasts.update(list => list.filter(t => t.id !== id));
  }

  private push(kind: ToastKind, text: string): void {
    const id = this.nextId++;
    this._toasts.update(list => [...list, { id, kind, text }]);
    setTimeout(() => this.dismiss(id), 4500);
  }
}
