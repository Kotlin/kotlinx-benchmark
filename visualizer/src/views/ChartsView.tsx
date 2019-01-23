import React, {Component} from "react";
import {ResultsBundle} from "../model/Data";

export class ChartsView extends Component<{ results: ResultsBundle }> {
    render() {
        return <pre>{JSON.stringify(this.props, null, 2)}</pre>;
    }
}