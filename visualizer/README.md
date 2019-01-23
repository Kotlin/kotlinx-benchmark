![2019-01-23 11 59 01](https://user-images.githubusercontent.com/2010355/51594803-5520f980-1f06-11e9-9a07-c99c0d8e2f0d.png)

This visualizer works as single page web application.
Benchmarks results may be placed in `reports` local dir or dropped in html page.

# Developing

Run `yarn start`, drop your files from `build/benchmarks/reports`.
Also files can be placed in `public/reports`. In this case it will loaded at startup.

# Build

Run `yarn build`. 

# Using in built app (discus?)

Place `build` files in `build/benchmarks`.
Http server should be start at `build/benchmarks` and `build/benchmarks/index.html` should be opened in browser.