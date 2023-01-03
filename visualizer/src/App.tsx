import React, {Component} from 'react';
import './App.css';
import {UploadView} from "./views/UploadView";
import {downloadBenchmarkResult, loadBenchmarkFromFiles, ResultsBundle} from "./model/Data";
import {ChartsView} from "./views/ChartsView";

class App extends Component<{}, { results: null | "loading" | ResultsBundle }> {
    constructor(props: Readonly<{}>) {
        super(props);

        this.waitBundle(downloadBenchmarkResult('reports'))
    }

    render() {
        if (!this.state || this.state.results == null) {
            return <UploadView onDropAccepted={
                (files) => this.waitBundle(loadBenchmarkFromFiles(files))
            }/>;
        } else if (this.state.results == "loading") {
            return "loading..."
        } else {
            return <ChartsView results={this.state.results as ResultsBundle}/>
        }
    }

    waitBundle(promise: Promise<ResultsBundle>) {
        this.setState({results: "loading"});
        promise
            .then(bundle => this.setState({results: bundle}))
            .catch(() => this.setState({results: null}));
    }
}

export default App;
