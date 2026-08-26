import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClientCompanyService } from '../../services/client-company.service';
import { ClientCompanyResponse } from '../../models/client-company.model';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-client-company-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './client-company-form.component.html'
})
export class ClientCompanyFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientCompanyService = inject(ClientCompanyService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly isEditMode = signal(false);
  readonly isViewOnly = signal(false);
  readonly companyId = signal<number | null>(null);
  readonly company = signal<ClientCompanyResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    companyCode: ['', Validators.required],
    companyName: ['', Validators.required],
    legalName: [''],
    email: ['', Validators.email],
    phone: [''],
    alternatePhone: [''],
    address: [''],
    city: [''],
    state: [''],
    country: [''],
    pincode: [''],
    contactPersonName: [''],
    contactPersonEmail: [''],
    contactPersonPhone: [''],
    createClientAdminLogin: [false],
    clientAdminLogin: this.fb.nonNullable.group({
      username: [''],
      password: ['']
    })
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isNewRoute = this.route.snapshot.url.some(seg => seg.path === 'new');

    this.form.controls.createClientAdminLogin.valueChanges.subscribe(enabled => {
      const group = this.form.controls.clientAdminLogin;
      if (enabled) {
        group.controls.username.setValidators([Validators.required, Validators.minLength(3)]);
        group.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
      } else {
        group.controls.username.clearValidators();
        group.controls.password.clearValidators();
      }
      group.controls.username.updateValueAndValidity();
      group.controls.password.updateValueAndValidity();
    });

    if (idParam) {
      this.companyId.set(Number(idParam));
      const isEditRoute = this.route.snapshot.url.some(seg => seg.path === 'edit');
      this.isEditMode.set(isEditRoute);
      this.isViewOnly.set(!isEditRoute);
      this.form.controls.companyCode.disable();
      this.loading.set(true);
      this.clientCompanyService.getById(Number(idParam)).subscribe({
        next: c => { this.company.set(c); this.patchForm(c); this.loading.set(false); },
        error: () => { this.toast.error('Unable to load client company.'); this.loading.set(false); }
      });
    } else if (isNewRoute) {
      this.isEditMode.set(false);
      this.form.controls.companyCode.disable();
      this.clientCompanyService.nextCode().subscribe({
        next: code => this.form.controls.companyCode.setValue(code),
        error: () => this.toast.error('Unable to generate the next company code.')
      });
    }
  }

  private patchForm(c: ClientCompanyResponse): void {
    this.form.patchValue({
      companyCode: c.companyCode,
      companyName: c.companyName,
      legalName: c.legalName ?? '',
      email: c.email ?? '',
      phone: c.phone ?? '',
      alternatePhone: c.alternatePhone ?? '',
      address: c.address ?? '',
      city: c.city ?? '',
      state: c.state ?? '',
      country: c.country ?? '',
      pincode: c.pincode ?? '',
      contactPersonName: c.contactPersonName ?? '',
      contactPersonEmail: c.contactPersonEmail ?? '',
      contactPersonPhone: c.contactPersonPhone ?? ''
    });
    if (this.isViewOnly()) this.form.disable();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const payload = {
      ...raw,
      clientAdminLogin: raw.createClientAdminLogin ? raw.clientAdminLogin : undefined
    };

    const isUpdate = this.companyId() !== null && this.isEditMode();
    const action$ = isUpdate
      ? this.clientCompanyService.update(this.companyId()!, payload)
      : this.clientCompanyService.create(payload);

    action$.subscribe({
      next: () => {
        this.toast.success(isUpdate ? 'Client company updated successfully.' : 'Client company created successfully.');
        this.saving.set(false);
        this.router.navigateByUrl('/clients');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save client company.');
      }
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/clients');
  }
}
