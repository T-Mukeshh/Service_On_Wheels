import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

const passwordsMatchValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value as string | undefined;
  const confirm = group.get('confirmPassword')?.value as string | undefined;
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  readonly form = this.fb.nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
    },
    { validators: passwordsMatchValidator },
  );

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update(v => !v);
  }

  onSubmit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const raw = this.form.getRawValue();
    this.auth
      .register({
        name: raw.name,
        email: raw.email,
        password: raw.password,
        phoneNumber: raw.phoneNumber,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('Account created!', 'Welcome to Service on Wheels.');
          void this.router.navigateByUrl('/dashboard');
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          const msg = extractHttpMessage(err);
          this.errorMessage.set(msg);
          this.toast.error('Registration failed', msg);
        },
      });
  }
}

function extractHttpMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as { message?: string; validationErrors?: Record<string, string> } | undefined;
    if (body?.validationErrors && typeof body.validationErrors === 'object') {
      const first = Object.values(body.validationErrors)[0];
      if (first) {
        return first;
      }
    }
    return body?.message ?? err.message ?? 'Registration failed';
  }
  return 'Something went wrong';
}
