# TableTurn Frontend

This is the standalone frontend for TableTurn. It uses plain HTML/CSS/JavaScript and calls the Spring Boot API at `http://localhost:8080/api`.

## Run

1. Start the Spring Boot backend first.
2. Open `index.html` directly in Chrome/Edge, or use VS Code Live Server.
3. If your backend uses another URL, open the browser console and run:

```js
localStorage.setItem('tableturnApiBase', 'http://localhost:8080/api')
```

Then refresh the page.
