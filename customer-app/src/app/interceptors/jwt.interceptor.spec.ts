import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AUTH_TOKEN_STORAGE_KEY } from '../models/auth-storage';
import { jwtInterceptor } from './jwt.interceptor';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  const storage = new Map<string, string>();

  beforeEach(() => {
    storage.clear();
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      value: {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => storage.set(key, value),
        removeItem: (key: string) => storage.delete(key),
      },
    });
    window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'test-token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    httpMock.verify();
  });

  it('attaches the token to protected Service on Wheels API requests', () => {
    http.get('http://localhost:8081/api/service/my-requests').subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/service/my-requests');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush([]);
  });

  it('does not leak the token to third-party requests', () => {
    http.get('https://nominatim.openstreetmap.org/reverse').subscribe();

    const req = httpMock.expectOne('https://nominatim.openstreetmap.org/reverse');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does not attach the token to public authentication requests', () => {
    http.post('http://localhost:8081/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('http://localhost:8081/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
