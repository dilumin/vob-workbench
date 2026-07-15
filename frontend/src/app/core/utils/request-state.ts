import { WritableSignal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { readableHttpError } from '../services/http-error';

export interface RequestState {
  loading: WritableSignal<boolean>;
  error: WritableSignal<string>;
}

export function handleRequest<T>(
  source: Observable<T>,
  state: RequestState,
  onSuccess: (value: T) => void,
): Subscription {
  state.loading.set(true);
  state.error.set('');

  return source.subscribe({
    next: (value) => {
      onSuccess(value);
      state.loading.set(false);
    },
    error: (error) => {
      state.error.set(readableHttpError(error));
      state.loading.set(false);
    },
  });
}

export function handleAction<T>(
  source: Observable<T>,
  state: RequestState,
  onSuccess: (value: T) => void,
): Subscription {
  return handleRequest(source, state, onSuccess);
}
