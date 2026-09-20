import { Component, AfterViewInit, ElementRef, Inject, PLATFORM_ID } from '@angular/core';
import { RouterLink } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class LandingPage implements AfterViewInit {
  private isBrowser: boolean;

  constructor(
    private el: ElementRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    /* ── 1. Progressive Scroll-reveal ───────────────── */
    const revealEls = Array.from(this.el.nativeElement.querySelectorAll('[data-reveal]')) as HTMLElement[];

    if (!prefersReducedMotion && typeof IntersectionObserver !== 'undefined') {
      const revealObs = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              const target = entry.target as HTMLElement;
              target.classList.remove('reveal-pending');
              target.classList.add('revealed');
              revealObs.unobserve(target);
            }
          });
        },
        { threshold: 0.05, rootMargin: '0px 0px 100px 0px' }
      );

      revealEls.forEach((el) => {
        const rect = el.getBoundingClientRect();
        // If element is below current viewport, enable reveal animation
        if (rect.top >= window.innerHeight) {
          el.classList.add('reveal-pending');
          revealObs.observe(el);
        } else {
          el.classList.add('revealed');
        }
      });
    } else {
      revealEls.forEach((el) => el.classList.add('revealed'));
    }

    /* ── 2. Stat count-up ────────────────────────────── */
    const statEls = Array.from(this.el.nativeElement.querySelectorAll('[data-count-target]')) as HTMLElement[];
    if (statEls.length > 0) {
      if (!prefersReducedMotion && typeof IntersectionObserver !== 'undefined') {
        const countObs = new IntersectionObserver(
          (entries) => {
            entries.forEach((entry) => {
              if (entry.isIntersecting) {
                this.animateCount(entry.target as HTMLElement, false);
                countObs.unobserve(entry.target);
              }
            });
          },
          { threshold: 0.1 }
        );
        statEls.forEach((el) => {
          const rect = el.getBoundingClientRect();
          if (rect.top < window.innerHeight && rect.bottom > 0) {
            this.animateCount(el, false);
          } else {
            countObs.observe(el);
          }
        });
      } else {
        statEls.forEach((el) => this.animateCount(el, true));
      }
    }

    /* ── 3. Hero dispatch route SVG draw-on-load ────── */
    if (!prefersReducedMotion) {
      const routePath = this.el.nativeElement.querySelector('.dispatch-route-draw') as SVGPathElement | null;
      if (routePath) {
        try {
          const len = routePath.getTotalLength() || 350;
          routePath.style.strokeDasharray = `${len}`;
          routePath.style.strokeDashoffset = `${len}`;
          requestAnimationFrame(() => {
            routePath.style.transition = 'stroke-dashoffset 1.6s ease-out 0.2s';
            routePath.style.strokeDashoffset = '0';
          });
        } catch {
          // Graceful fallback
        }
      }
    }
  }

  private animateCount(el: HTMLElement, skipAnimation: boolean): void {
    const raw = el.getAttribute('data-count-target') ?? '0';
    const prefix = el.getAttribute('data-count-prefix') ?? '';
    const suffix = el.getAttribute('data-count-suffix') ?? '';

    if (skipAnimation) {
      el.textContent = prefix + raw + suffix;
      return;
    }

    const numericStr = raw.replace(/[^0-9.]/g, '');
    const target = parseFloat(numericStr);
    if (isNaN(target)) {
      el.textContent = prefix + raw + suffix;
      return;
    }

    const isDecimal = raw.includes('.');
    const hasComma = raw.includes(',');
    const duration = 1200;
    const start = performance.now();

    const tick = (now: number) => {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = eased * target;

      let formatted: string;
      if (isDecimal) {
        formatted = current.toFixed(1);
      } else {
        const rounded = Math.round(current);
        formatted = hasComma ? rounded.toLocaleString('en-IN') : String(rounded);
      }

      el.textContent = prefix + formatted + suffix;

      if (progress < 1) {
        requestAnimationFrame(tick);
      } else {
        el.textContent = prefix + raw + suffix;
      }
    };
    requestAnimationFrame(tick);
  }
}
