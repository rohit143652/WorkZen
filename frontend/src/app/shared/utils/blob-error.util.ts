import { HttpErrorResponse } from '@angular/common/http';

/**
 * When an HttpClient request is made with `responseType: 'blob'` (any file download - PDF,
 * Excel, etc.), and the server responds with an error, Angular does NOT parse that error body as
 * JSON - `err.error` is still a raw Blob, even though the server actually sent back a normal JSON
 * error object (e.g. `{ message: "..." }`). Reading `err.error?.message` in that case is always
 * undefined, no matter what the backend actually said, silently hiding the real reason behind
 * whatever generic fallback text the calling code wrote instead.
 *
 * This reads that Blob's text and parses it as JSON to recover the real message, falling back to
 * the provided default only if the body genuinely isn't parseable JSON (e.g. a raw network
 * failure with no response body at all).
 */
export async function extractBlobErrorMessage(err: HttpErrorResponse, fallback: string): Promise<string> {
  if (err.error instanceof Blob) {
    try {
      const text = await err.error.text();
      const parsed = JSON.parse(text);
      return parsed?.message ?? fallback;
    } catch {
      return fallback;
    }
  }
  return err.error?.message ?? fallback;
}
