import { HttpErrorResponse } from '@angular/common/http';

export function readableHttpError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.error?.message) {
      return error.error.message;
    }

    if (typeof error.error === 'string') {
      return error.error;
    }

    if (error.status === 0) {
      return 'Could not reach the backend. Start Spring Boot on port 8080.';
    }

    return `Request failed with status ${error.status}.`;
  }

  return 'Something went wrong.';
}
