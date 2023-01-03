import React, {Component} from "react";
import {ResultsBundle} from "../model/Data";
import {Bar, BarChart, BarProps, CartesianGrid, Legend, Tooltip, XAxis, YAxis} from "recharts";
import {colors} from "./colors";

export class ChartsView extends Component<{ results: ResultsBundle }> {
    render() {
        const data: any[] = [
            // {name: 'Page A', uv: 4000, pv: 2400, amt: 2400},
            // {name: 'Page B', uv: 3000, pv: 1398, amt: 2210},
            // {name: 'Page C', uv: 2000, pv: 9800, amt: 2290},
            // {name: 'Page D', uv: 2780, pv: 3908, amt: 2000},
            // {name: 'Page E', uv: 1890, pv: 4800, amt: 2181},
            // {name: 'Page F', uv: 2390, pv: 3800, amt: 2500},
            // {name: 'Page G', uv: 3490, pv: 4300, amt: 2100},
        ];

        const bars: React.ReactElement<BarProps>[] = [];
        const benchmarkValues: {[name: string]: any} = {};
        let i = 0;

        function getBenchValues(name: string) {
            if (benchmarkValues[name]) {
                return benchmarkValues[name]
            } else {
                const obj = {
                    name: name
                };

                benchmarkValues[name] = obj;
                data.push(obj);

                return obj;
            }
        }

        /**
         * Remove platform name from benchmark name.
         * Example: test.NativeTestBenchmark.sqrtBenchmark -> test.TestBenchmark.sqrtBenchmark
         */
        function cleanBenchmarkName(platform: string, name: string): string {
            return name.replace(new RegExp(platform, "ig"), '')
        }

        for (let target in this.props.results) {
            bars.push(<Bar dataKey={target} fill={colors[i++]}/>);

            const dataValues: any = {name: target};
            this.props.results[target].forEach(bench => {
                let benchmarkName = cleanBenchmarkName(target, bench.benchmark);
                getBenchValues(benchmarkName)[target] = bench.primaryMetric.score
            });
        }

        return <BarChart width={600} height={300} data={data}
                         margin={{top: 5, right: 30, left: 20, bottom: 5}}>
            <CartesianGrid strokeDasharray="3 3"/>
            <XAxis dataKey="name"/>
            <YAxis/>
            <Tooltip/>
            <Legend/>
            {bars}
        </BarChart>

        //return <pre>{JSON.stringify(this.props, null, 2)}</pre>;
    }
}