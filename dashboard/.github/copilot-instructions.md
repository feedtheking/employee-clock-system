# Copilot Instructions for This Codebase

## Overview
This project is a React single-page application (SPA) bootstrapped with Vite. It uses modern React (function components, hooks) and is organized for clarity and modularity. The main entry point is `src/main.jsx`, which loads the root `App` component. Navigation between pages is handled by rendering different page components (e.g., `HomePage`, `EmployeePage`, `LogsPage`) from `src/`.

## Key Architectural Patterns
- **Component Structure:**
  - All main pages are in `src/` as `*Page.jsx` files (e.g., `EmployeePage.jsx`, `LogsPage.jsx`).
  - Shared UI elements (e.g., `NavBar.jsx`) are also in `src/`.
  - Styles are colocated as `.css` files (e.g., `App.css`, `index.css`).
- **State Management:**
  - State is managed locally within components using React hooks (`useState`, `useEffect`).
  - No global state management library is present.
- **External Services:**
  - Firebase integration is set up in `src/firebase.js`. All Firebase-related logic should be imported from this file.
- **Assets:**
  - Static assets (SVGs, images) are in `src/assets/` and `public/`.

## Developer Workflows
- **Development:**
  - Start the dev server: `npm run dev`
  - Vite provides fast HMR (Hot Module Replacement).
- **Build:**
  - Production build: `npm run build`
  - Preview production build: `npm run preview`
- **Linting:**
  - Lint code: `npm run lint`
  - ESLint config is in `eslint.config.js`.
- **No explicit test setup** is present; add tests as needed.

## Project-Specific Conventions
- **Page Components:**
  - Each main page is a top-level React component in `src/` named `*Page.jsx`.
  - Example: `EmployeePage.jsx` renders employee-related UI and logic.
- **Navigation:**
  - Navigation is handled manually or via a shared `NavBar` component, not via React Router.
- **Firebase Usage:**
  - All Firebase imports should come from `src/firebase.js` to ensure consistent configuration.
- **Styling:**
  - Use the provided CSS files for styling; avoid inline styles for large UI blocks.

## Integration Points
- **Firebase:**
  - All authentication, database, or storage logic should be centralized in `src/firebase.js`.
- **Assets:**
  - Use `src/assets/` for React-imported assets and `public/` for static files referenced in `index.html`.

## Examples
- To add a new page, create `src/NewPage.jsx` and add it to the navigation logic in `App.jsx` and/or `NavBar.jsx`.
- To use Firebase, import from `src/firebase.js`:
  ```js
  import { db } from './firebase';
  ```

## References
- Main entry: `src/main.jsx`
- App shell: `src/App.jsx`
- Firebase config: `src/firebase.js`
- Navigation: `src/NavBar.jsx`, `src/App.jsx`
- Styles: `src/App.css`, `src/index.css`

---
_If you are an AI agent, follow these conventions and reference the above files for examples of project patterns._
