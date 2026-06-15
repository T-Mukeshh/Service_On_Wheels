export const API_BASE_URL = 'http://localhost:8081';

export function isServiceApiUrl(url: string): boolean {
  return url.startsWith(`${API_BASE_URL}/api/`);
}
