import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SiteService } from '../../services/site.service';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-site-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './site-form.component.html'
})
export class SiteFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly siteService = inject(SiteService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly saving = signal(false);
  readonly isEditMode = signal(false);
  readonly siteId = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    siteCode: [''],
    siteName: ['', Validators.required],
    description: [''],
    address: [''],
    city: [''],
    state: [''],
    country: [''],
    pincode: [''],
    siteContactPerson: [''],
    siteContactNumber: [''],
    requiredEmployeeCount: [0, [Validators.required, Validators.min(0)]],
    allowOverAllocation: [false]
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.siteId.set(Number(idParam));
      this.form.controls.siteCode.disable();
      this.siteService.getById(Number(idParam)).subscribe(site => this.form.patchValue(site));
    } else {
      this.form.controls.siteCode.disable();
      this.siteService.nextCode().subscribe({
        next: code => this.form.controls.siteCode.setValue(code),
        error: () => this.toast.error('Unable to generate the next site code.')
      });
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    const action$ = this.isEditMode()
      ? this.siteService.update(this.siteId()!, raw)
      : this.siteService.create(raw);
    action$.subscribe({
      next: () => { this.toast.success('Site saved successfully.'); this.saving.set(false); this.router.navigateByUrl('/sites'); },
      error: (err: HttpErrorResponse) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save site.'); }
    });
  }
}
