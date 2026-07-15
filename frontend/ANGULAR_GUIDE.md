# Angular Guide: Zero To Confident

This guide teaches Angular using this project as the reference app.

The goal is not to memorize everything at once. The goal is to understand Angular's mental model, then learn where each idea appears in real code.

This project uses modern Angular with:

- standalone components
- routes with lazy-loaded pages
- services
- dependency injection
- HTTP client
- route guards
- HTTP interceptor
- signals
- reactive forms
- SCSS

---

## 1. What Angular Is

Angular is a frontend application framework.

It helps you build large browser apps by organizing code into:

- components: pieces of UI
- templates: HTML connected to component state
- services: reusable business/API logic
- routes: page navigation
- forms: user input handling
- dependency injection: clean way to share objects
- build tooling: compile, test, serve, bundle

Angular apps are usually TypeScript apps.

---

## 2. The Big Mental Model

Angular is built around this loop:

```text
User sees template
        |
User clicks/types/navigates
        |
Component method runs
        |
State changes
        |
Template updates
```

Example:

```ts
readonly requests = signal<VobRequest[]>([]);
```

Template:

```html
@for (request of requests(); track request.id) {
  <tr>{{ request.status }}</tr>
}
```

When `requests.set(...)` runs, Angular updates the screen.

---

## 3. Project Structure

This app is organized like this:

```text
src/app
  app.ts
  app.routes.ts
  app.config.ts

  core
    guards
    interceptors
    models
    services
    utils

  layout
    app-shell

  shared
    components

  features
    auth
    dashboard
    patients
    vobs
    admin
    audit
```

### `app.ts`

The root component. It only renders the router.

### `app.routes.ts`

Defines app pages:

- `/login`
- `/dashboard`
- `/patients`
- `/vobs`
- `/vobs/:id`
- `/admin`
- `/audit`

### `core`

Shared app logic that is not a visual page.

Examples:

- API models
- services
- guards
- auth interceptor
- permission rules
- request helpers

### `layout`

The main shell around protected pages:

- sidebar
- topbar
- sign out button
- page outlet

### `shared`

Reusable UI components:

- modal
- confirm modal
- page header
- status badge
- empty state

### `features`

Actual pages/screens.

Each feature owns its component and template.

---

## 4. TypeScript Basics You Need

Angular code is TypeScript.

### Variables

```ts
const name = 'Angular';
let count = 0;
```

Use `const` unless you need to reassign.

### Types

```ts
let username: string = 'specialist1';
let total: number = 10;
let active: boolean = true;
```

### Interfaces

Interfaces describe object shapes.

```ts
export interface Patient {
  id: string;
  mrn: string;
  firstName: string;
  lastName: string;
}
```

This app stores backend types in:

```text
src/app/core/models/api.types.ts
```

### Union Types

```ts
export type Role = 'RECEPTIONIST' | 'SPECIALIST' | 'SUPERVISOR_ADMIN' | 'VIEWER';
```

This means `Role` can only be one of those exact strings.

---

## 5. Components

A component is a UI building block.

Example:

```ts
@Component({
  selector: 'app-patients',
  imports: [CommonModule, FormsModule],
  templateUrl: './patients.component.html',
})
export class PatientsComponent {}
```

### Component Parts

```text
patients.component.ts     logic/state
patients.component.html   UI template
patients.component.scss   component-specific styles, optional
```

### Standalone Components

This app uses standalone components.

That means each component imports what it needs directly:

```ts
imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent]
```

Older Angular apps often use `NgModule`. Modern Angular can avoid most modules.

---

## 6. Templates

Templates are Angular HTML.

They can show values:

```html
<h1>{{ title }}</h1>
```

They can respond to events:

```html
<button (click)="load()">Refresh</button>
```

They can bind properties:

```html
<button [disabled]="saving()">Save</button>
```

They can bind two-way input values:

```html
<input [(ngModel)]="searchTerm" />
```

---

## 7. Angular Control Flow

Modern Angular uses `@if` and `@for`.

### `@if`

```html
@if (error()) {
  <div class="alert error">{{ error() }}</div>
}
```

### `@for`

```html
@for (patient of patients(); track patient.id) {
  <tr>
    <td>{{ patient.mrn }}</td>
    <td>{{ patient.firstName }} {{ patient.lastName }}</td>
  </tr>
}
```

Always use `track` for lists.

Good:

```html
@for (item of items(); track item.id) {}
```

Avoid:

```html
@for (item of items()) {}
```

Tracking helps Angular update lists efficiently.

---

## 8. Signals

Signals are Angular's modern state primitive.

Signal state:

```ts
readonly patients = signal<Patient[]>([]);
readonly loading = signal(false);
readonly error = signal('');
```

Read a signal:

```ts
this.patients()
```

Set a signal:

```ts
this.patients.set(page.content);
```

Update from old value:

```ts
this.patients.update((items) => [...items, newPatient]);
```

Template usage:

```html
@if (loading()) {
  Loading...
}

@for (patient of patients(); track patient.id) {
  {{ patient.firstName }}
}
```

### Why Signals Are Good

Signals make UI state explicit and easy to follow.

Instead of:

```ts
patients: Patient[] = [];
```

Use:

```ts
readonly patients = signal<Patient[]>([]);
```

This makes Angular update the UI reliably when state changes.

---

## 9. Services

Services hold reusable logic.

Components should not directly know every API detail.

Example service:

```ts
@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
}
```

This app has services in:

```text
src/app/core/services
```

Examples:

- `auth.service.ts`
- `patient.service.ts`
- `vob-request.service.ts`
- `admin.service.ts`
- `audit.service.ts`
- `permissions.service.ts`

### Why Services Matter

Bad:

```ts
// Component directly builds every API URL everywhere.
this.http.get('http://localhost:8080/api/patients');
```

Good:

```ts
this.patientService.search(this.searchTerm);
```

The component says what it wants. The service knows how to get it.

---

## 10. Dependency Injection

Dependency injection lets Angular give a class what it needs.

Modern style:

```ts
private readonly patientService = inject(PatientService);
private readonly fb = inject(FormBuilder);
```

Older style:

```ts
constructor(private patientService: PatientService) {}
```

Both work. This app uses `inject()`.

---

## 11. HTTP Client

Angular uses `HttpClient` for API calls.

Setup happens in:

```text
src/app/app.config.ts
```

```ts
provideHttpClient(withInterceptors([authInterceptor]))
```

Example service method:

```ts
search(q = '') {
  const params = new HttpParams().set('page', 0).set('size', 20).set('q', q);
  return this.http.get<PageResponse<Patient>>(`${this.baseUrl}/patients`, { params });
}
```

Important: HTTP methods return Observables.

```ts
this.patientService.search('john').subscribe(...)
```

In this app, most components use the helper:

```ts
handleRequest(this.patientService.search(this.searchTerm), this, (page) => {
  this.patients.set(page.content);
});
```

---

## 12. Observables

An Observable is a stream of values.

HTTP Observables usually emit once and complete.

Example:

```ts
this.http.get<Patient[]>('/api/patients')
```

To run it:

```ts
.subscribe({
  next: (data) => {},
  error: (error) => {},
});
```

This app wraps common subscribe behavior in:

```text
src/app/core/utils/request-state.ts
```

That file handles:

- set loading true
- clear old error
- update state on success
- convert backend errors into readable text
- set loading false

---

## 13. Routing

Routes define pages.

File:

```text
src/app/app.routes.ts
```

Example:

```ts
{
  path: 'patients',
  loadComponent: () =>
    import('./features/patients/patients.component').then((m) => m.PatientsComponent),
}
```

This is lazy loading. Angular loads the page code only when needed.

### Router Outlet

Root app:

```html
<router-outlet />
```

The app shell also has:

```html
<router-outlet />
```

This lets child pages render inside the layout.

---

## 14. Route Params

The VOB detail page uses:

```text
/vobs/:id
```

Example URL:

```text
/vobs/abc123
```

Read route param:

```ts
this.route.paramMap.subscribe((params) => {
  this.load(params.get('id'));
});
```

In this app:

```text
src/app/features/vobs/vob-detail.component.ts
```

---

## 15. Query Params

Query params look like this:

```text
/vobs?patientId=abc123
```

The VOB list watches `patientId` and opens the New VOB modal with that patient selected.

```ts
this.route.queryParamMap.subscribe((params) => {
  const patientId = params.get('patientId');
});
```

---

## 16. Guards

Guards control route access.

Files:

```text
src/app/core/guards
```

### Auth Guard

Stops unauthenticated users from entering the app.

### Guest Guard

Stops logged-in users from going back to login.

### Role Guard

Restricts admin pages.

Example route:

```ts
{
  path: 'admin',
  canActivate: [roleGuard],
  data: { roles: ['SUPERVISOR_ADMIN'] },
}
```

---

## 17. Interceptors

Interceptors modify HTTP requests/responses globally.

File:

```text
src/app/core/interceptors/auth.interceptor.ts
```

It attaches the JWT token:

```ts
Authorization: Bearer token
```

This means components do not need to manually add auth headers.

---

## 18. Forms

Angular has two common form styles:

1. template-driven forms
2. reactive forms

This app mostly uses reactive forms.

Example:

```ts
readonly form = this.fb.nonNullable.group({
  firstName: ['', Validators.required],
  lastName: ['', Validators.required],
});
```

Template:

```html
<form [formGroup]="form" (ngSubmit)="save()">
  <input formControlName="firstName" />
</form>
```

### Why Reactive Forms Are Good

They are better for:

- modals
- validation
- complex forms
- testing
- predictable state

---

## 19. Validation

Example:

```ts
firstName: ['', Validators.required]
```

Check before saving:

```ts
if (this.form.invalid) {
  this.form.markAllAsTouched();
  return;
}
```

Current forms are simple. A future improvement is showing field-level validation messages.

---

## 20. Auth Flow

Login page:

```text
src/app/features/auth/login.component.ts
```

Auth service:

```text
src/app/core/services/auth.service.ts
```

Flow:

```text
User submits username/password
        |
AuthService calls /api/auth/login
        |
Backend returns JWT + refresh token
        |
AuthService stores session
        |
Interceptor adds JWT to later API calls
        |
Router navigates to dashboard
```

---

## 21. Permission Rules

Frontend permission rules live in:

```text
src/app/core/services/permissions.service.ts
```

Example:

```ts
canCreatePatient(): boolean {
  return this.auth.hasRole(['RECEPTIONIST', 'SUPERVISOR_ADMIN']);
}
```

Templates use permissions to hide actions:

```html
@if (permissions.canCreatePatient()) {
  <button>New patient</button>
}
```

Important: frontend permissions improve UX, but backend permissions are still the real security boundary.

---

## 22. Shared Components

Shared components avoid repeated UI code.

Current shared components:

```text
shared/components/modal
shared/components/confirm-modal
shared/components/page-header
shared/components/status-badge
shared/components/empty-state
```

### When To Create A Shared Component

Create one when:

- the same UI repeats in 3 or more places
- the logic is generic
- it improves readability

Do not create one too early.

Bad abstraction:

```text
One component used once.
```

Good abstraction:

```text
Modal used by patients, VOBs, admin, audit.
```

---

## 23. Styling

Global styles live in:

```text
src/styles.scss
```

Component-specific styles live next to components:

```text
component.scss
```

This app keeps most base UI styles global:

- buttons
- panels
- tables
- forms
- alerts
- loading lines
- layout utilities

### CSS Design Rules

Use:

- consistent spacing
- few colors
- predictable table layout
- clear form labels
- short button labels
- obvious empty states

Avoid:

- random one-off colors
- decorative gradients everywhere
- oversized headings in dashboards
- nested card piles
- hidden primary actions

---

## 24. Lifecycle Hooks

Common lifecycle hook:

```ts
ngOnInit(): void {
  this.load();
}
```

Use `ngOnInit` for initial data loading.

Avoid doing heavy work in constructors.

Constructor should mostly set up injected dependencies.

---

## 25. Lazy Loading

This route:

```ts
loadComponent: () => import('./features/patients/patients.component')
```

Means the patients page code loads only when user visits `/patients`.

Benefits:

- faster initial app load
- better feature separation
- cleaner routing

---

## 26. Error Handling

Backend errors are converted to user-friendly messages in:

```text
src/app/core/services/http-error.ts
```

Usage:

```ts
state.error.set(readableHttpError(error));
```

This keeps error display consistent.

---

## 27. Request State Helper

File:

```text
src/app/core/utils/request-state.ts
```

Instead of repeating:

```ts
loading = true;
error = '';
api.subscribe({
  next: ...
  error: ...
});
```

Use:

```ts
handleRequest(apiCall, this, (result) => {
  this.items.set(result.content);
});
```

This keeps components cleaner.

---

## 28. Testing

Run tests:

```bash
npm test
```

Current test:

```text
src/app/app.spec.ts
```

As the app grows, add tests for:

- guards
- permission service
- services
- important component behavior

---

## 29. Build

Build production bundle:

```bash
npm run build
```

Run dev server:

```bash
npm start
```

Open:

```text
http://localhost:4200
```

---

## 30. Debugging Angular

### Template Not Updating

Use signals for state:

```ts
readonly items = signal<Item[]>([]);
items.set(newItems);
```

### API Fails

Check:

- backend is running
- token is valid
- network tab in browser devtools
- backend CORS config
- endpoint URL

### Route Not Loading

Check:

- route path in `app.routes.ts`
- guard result
- browser URL
- console errors

### Form Not Submitting

Check:

- `[formGroup]="form"`
- `(ngSubmit)="save()"`
- `formControlName`
- validation errors

---

## 31. Angular Best Practices

### Keep Components Focused

A component should handle UI state and user actions.

Do not put raw API URL logic in components.

### Use Services For API Calls

Good:

```ts
this.patientService.search(...)
```

Bad:

```ts
this.http.get('http://localhost:8080/api/patients')
```

### Use Typed Models

Good:

```ts
Patient
VobRequest
DashboardSummary
```

Bad:

```ts
any
```

Use `any` only when you truly do not know the shape.

### Keep Templates Readable

If the template gets too complex, move logic into the component.

### Avoid Premature Abstraction

Do not create a generic component for everything.

Repeat twice, abstract third time.

### Keep UI Permissions And Backend Permissions Aligned

Frontend hides buttons for UX.

Backend enforces security.

Both should match.

---

## 32. Current App Feature Map

### Login

```text
features/auth
core/services/auth.service.ts
core/interceptors/auth.interceptor.ts
```

### Dashboard

```text
features/dashboard
core/services/admin.service.ts
core/services/vob-request.service.ts
```

### Patients

```text
features/patients
core/services/patient.service.ts
```

### VOB Requests

```text
features/vobs
core/services/vob-request.service.ts
```

### Admin

```text
features/admin
core/services/admin.service.ts
```

### Audit

```text
features/audit
core/services/audit.service.ts
```

---

## 33. Suggested Learning Path

### Phase 1: Basics

Learn:

- TypeScript basics
- components
- templates
- property binding
- event binding
- `@if`
- `@for`
- signals

Practice:

- change text on a page
- add a button
- add a signal counter
- render a list

### Phase 2: Forms

Learn:

- reactive forms
- validators
- form groups
- submit handlers

Practice:

- add validation messages to patient form
- disable save until form is valid
- reset form after save

### Phase 3: Services And HTTP

Learn:

- services
- dependency injection
- HttpClient
- Observables
- error handling

Practice:

- add a health check service
- add loading state
- add error state

### Phase 4: Routing

Learn:

- routes
- route params
- query params
- router links
- nested routes
- lazy loading

Practice:

- add a patient detail page
- link patient row to detail page
- read patient ID from route param

### Phase 5: Auth And Guards

Learn:

- JWT basics
- route guards
- interceptors
- role-based UI

Practice:

- hide a button by role
- guard a route by role
- show current user in topbar

### Phase 6: Advanced Angular

Learn:

- computed signals
- effects
- reusable components
- custom directives
- pipes
- RxJS operators
- component testing
- performance profiling

Practice:

- create a search input with debounce
- create a reusable table component
- test `PermissionsService`

---

## 34. Exercises For This App

### Easy

1. Add field-level validation messages to patient form.
2. Add a loading spinner style.
3. Add a "Clear filters" button to VOB worklist.
4. Add a count of pending requests on dashboard.

### Medium

1. Add patient detail page.
2. Add search debounce to patients.
3. Add pagination controls.
4. Add toast notifications.
5. Add role labels in topbar.

### Hard

1. Add refresh-token auto renewal.
2. Add optimistic locking conflict UI.
3. Add reusable data-table component.
4. Add unit tests for every service.
5. Add end-to-end tests.

---

## 35. Common Beginner Mistakes

### Forgetting To Import A Component

Standalone components need imports:

```ts
imports: [ModalComponent]
```

### Forgetting To Call A Signal

Wrong:

```html
{{ loading }}
```

Right:

```html
{{ loading() }}
```

### Mutating Arrays Directly

Avoid:

```ts
this.items().push(item);
```

Use:

```ts
this.items.update((items) => [...items, item]);
```

### Putting Too Much In Templates

If an expression gets hard to read, move it into TypeScript.

### Using `any`

Avoid `any` unless absolutely needed.

---

## 36. How To Read This Codebase

Start here:

1. `src/app/app.routes.ts`
2. `src/app/layout/app-shell/app-shell.component.html`
3. `src/app/features/patients/patients.component.ts`
4. `src/app/core/services/patient.service.ts`
5. `src/app/core/models/api.types.ts`

Then read:

1. `src/app/features/vobs/vob-list.component.ts`
2. `src/app/features/vobs/vob-detail.component.ts`
3. `src/app/core/services/permissions.service.ts`
4. `src/app/core/interceptors/auth.interceptor.ts`
5. `src/app/core/guards/auth.guard.ts`

---

## 37. A Good Angular Component Checklist

Before calling a component done:

- Does it have a clear purpose?
- Is state explicit?
- Are API calls in a service?
- Are permissions checked where actions are shown?
- Does it show loading state?
- Does it show error state?
- Does it show empty state?
- Are forms validated?
- Is the template readable?
- Are repeated UI patterns extracted?
- Does `npm run build` pass?
- Does `npm test` pass?

---

## 38. What To Learn After Angular

Once Angular feels comfortable, learn:

- accessibility
- browser devtools
- HTTP caching
- frontend security basics
- design systems
- testing-library style tests
- Playwright or Cypress
- performance profiling
- deployment

Angular is only one part of frontend engineering. Great frontend work also needs product thinking, UX judgment, accessibility, and API understanding.

---

## 39. Final Mental Model

Remember this:

```text
Routes choose the page.
Components own UI behavior.
Templates render component state.
Signals store reactive state.
Services talk to the backend.
Guards protect routes.
Interceptors modify HTTP globally.
Shared components remove repeated UI.
```

If you understand that, Angular becomes much less mysterious.

---

## 40. How This App Starts In The Browser

When you run:

```bash
npm start
```

or:

```bash
ng serve
```

Angular builds the app and serves it at:

```text
http://localhost:4200
```

The startup chain is:

```text
index.html
  -> main.ts
  -> app.config.ts
  -> app.ts
  -> app.routes.ts
  -> current page component
```

In plain language:

- `index.html` is the browser entry page.
- `main.ts` starts Angular.
- `app.config.ts` registers global providers like routing and HTTP.
- `app.ts` is the root component.
- `app.routes.ts` decides which page to show for the current URL.
- Feature components render the actual screens.

When you feel lost, follow this chain from top to bottom.

---

## 41. Your Learning Roadmap

Use this order. It is much easier than trying to learn everything randomly.

### Stage 1: Comfortable Beginner

Learn:

- components
- templates
- interpolation with `{{ value }}`
- property binding with `[value]="x"`
- event binding with `(click)="doSomething()"`
- `@if`
- `@for`
- basic services
- basic routing

Practice:

- Change labels in a page.
- Add one new button.
- Add one new readonly field to a page.
- Add one new route that displays static text.

### Stage 2: Real App Builder

Learn:

- reactive forms
- validators
- HTTP services
- loading states
- error states
- modals
- route parameters
- query parameters
- shared components

Practice:

- Add a new form field to patient creation.
- Show a new backend field in a table.
- Add an empty state to a list page.
- Add a confirmation modal before a risky action.

### Stage 3: Strong Angular Developer

Learn:

- guards
- interceptors
- dependency injection deeply
- signals
- computed state
- component input/output patterns
- accessibility
- testing
- folder architecture

Practice:

- Add a role-based action.
- Add a new API service method.
- Write a unit test for a service.
- Extract repeated UI into a shared component.

### Stage 4: Professional Frontend Engineer

Learn:

- design systems
- keyboard accessibility
- performance
- test strategy
- API contract design
- state management tradeoffs
- deployment
- observability and logging

Practice:

- Audit every page for loading, error, empty, and success states.
- Test a complete user workflow.
- Improve mobile layout.
- Replace duplicated styles with shared utilities.

---

## 42. Exercises For This VOB App

Try these in order.

1. Add a `middleName` field to the patient form.
2. Show the patient's full name in one computed helper.
3. Add a filter to the VOB list.
4. Add a loading message to one page.
5. Create a reusable `FormFieldErrorComponent`.
6. Add an admin-only button and hide it from non-admin users.
7. Add a new page called `reports`.
8. Add a route guard to protect that page.
9. Add a service method for one backend endpoint.
10. Write one test for that service.

For every exercise, ask yourself:

- Which component owns the screen?
- Which service talks to the backend?
- Which model describes the data?
- Which route opens the page?
- What should the user see while loading?
- What should the user see if the backend fails?

That thinking pattern is how professional Angular work becomes calm.

---

## 43. How To Experiment Safely

Because you are learning, you should feel free to break things.

Before a big experiment, check the current state:

```bash
git status
```

After changing code, test:

```bash
npm run build
npm test
```

If you want to go back to the clean Angular-init style later, keep the generated files easy to identify:

- root app files live in `src/app`
- feature pages live in `src/app/features`
- reusable app code lives in `src/app/core`
- reusable UI lives in `src/app/shared`
- shell layout lives in `src/app/layout`

This structure makes it easier to remove app-specific code while keeping the Angular project itself.

The best way to learn Angular is not to read this guide once. Read one section, change one small thing, run the app, and repeat.
