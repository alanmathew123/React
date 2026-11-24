// utils/greeting.js
export function greeting(name) {
  return `Hello, ${name}!`;
}

// App.jsx
import { greeting } from "./utils/greeting";

export default function App() {
  return <h1>{greeting("Alan")}</h1>;
}