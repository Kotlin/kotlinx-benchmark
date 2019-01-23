import React, {Component} from 'react';
import './App.css';
import {UploadView} from "./UploadView";
import {loadBenchmarkFromFiles} from "./Data";

class App extends Component {


    render() {
    return (
        <UploadView onDropAccepted={
            (files) => loadBenchmarkFromFiles(files).then(bundle => console.log(bundle))
        }/>
    );
  }
}

export default App;
