import React, {Component} from "react";
import {blue, green} from "./colors";
import {FaUpload} from "react-icons/fa";
import Dropzone, {DropFileEventHandler} from "react-dropzone";

export class UploadView extends Component<{ onDropAccepted: DropFileEventHandler }> {
    render() {
        return <div className="App">
            {/*
            // @ts-ignore */}
            <Dropzone
                onDropAccepted={this.props.onDropAccepted}
                onDropRejected={() => alert('Only drop valid JSON files!')}
                multiple={true}
                accept='.json'
                disableClick={true}
                disablePreview={true}
                rejectStyle={{borderColor: green, borderWidth: 3, borderStyle: "dotted"}}
                activeStyle={{borderColor: green, borderWidth: 3, borderStyle: "dotted"}}>
                {({getRootProps, getInputProps}) => (
                    <div
                        {...getRootProps()}
                        className="container-fluid"
                        style={{
                            height: "81vh",
                            borderWidth: 1,
                            borderColor: blue,
                            borderStyle: "dashed",
                            borderRadius: 25,
                            padding: 20,
                            textAlign: "center",
                            verticalAlign: "middle",
                            marginRight: "auto",
                            marginLeft: "auto"
                        }}
                    >
                        <div>
                            <h1 style={{marginBottom: 20}}>Dropzone</h1>
                            <h5>Drop your JSON report files here!</h5>
                        </div>
                        <h2><FaUpload width={100} height={100}/></h2>
                        <br/>
                        <div>
                            Use this tool to visually explore your benchmark results!
                            Simply upload<sup>*</sup> any result files from build/benchmarks/reports.
                        </div>
                        <br/>
                        <div style={{fontSize: 12, textAlign: "center"}}>
                            * Your data stays locally in your browser, it is not send to any server!
                        </div>
                    </div>
                )}
            </Dropzone>
        </div>;
    }
}