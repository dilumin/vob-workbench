# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.0.4.

New to Angular? Start with [ANGULAR_GUIDE.md](./ANGULAR_GUIDE.md). It explains Angular from beginner concepts through the patterns used in this app.

## VOB Workbench Angular UI

This app is structured as a realistic Angular frontend for the Spring Boot backend.

Main folders:

- `src/app/core` contains app-wide code: API models, HTTP services, auth, interceptors, and route guards.
- `src/app/shared` contains reusable UI components such as modals, page headers, empty states, and status badges.
- `src/app/layout` contains the protected application shell with sidebar navigation.
- `src/app/features` contains routed pages: login, dashboard, patients, VOB requests, admin, and audit.

Important Angular concepts used:

- standalone components
- lazy-loaded routes with `loadComponent`
- route guards for login and admin-only pages
- HTTP interceptor for bearer tokens
- reactive forms for create/edit modals
- template control flow with `@if` and `@for`
- services for backend API calls

Run the backend first on `http://localhost:8080`, then start this frontend.

Seed users:

```text
receptionist1 / password123
specialist1   / password123
admin1        / password123
viewer1       / password123
```

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
