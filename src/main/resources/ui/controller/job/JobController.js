/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class JobController {
    constructor(controller) {
        this._controller = controller;
        this._model = this._controller._model;
        this._model.setProperty("/jobTypes", this.jobTypes);
    }

    onBeforeOpen() {
        this._prepareJobData();

        this._dialog = this._controller.byId("jobDialog");
        this._ruleForm = this._controller.byId("jobForm");

        this.onJobTypeSelect(true)
    }

    onJobSave() {
        let jobDefinitionsPath = "/workflow/dagDefinitionJoined/jobDefinitions";
        let jobDefinitions = this._model.getProperty(jobDefinitionsPath);

        if (this._model.getProperty("/newJob/isEdit")) {
            this._onEditSave(jobDefinitionsPath, jobDefinitions);
        } else {
            this._onCreateSave(jobDefinitionsPath, jobDefinitions);
        }
        this.onClosePress();
    }

    onJobTypeSelect(isInitial) {
        isInitial !== true && this._model.setProperty("/newJob/job/jobParameters/", jQuery.extend(true, {}, this._emptyJobParameters));
        this._showFragment()
    }

    _showFragment() {
        let selectedJobType = this._model.getProperty("/newJob/job/jobType/name");
        let fragmentName = this.jobTypes.find(function(e) { return e.name === selectedJobType }).fragment;
        let controllerName = this.jobTypes.find(function(e) { return e.name === selectedJobType }).controller;
        FragmentMethods.showFragmentIn(controllerName, fragmentName, "hyperdriver.view.job", this._ruleForm, this._model);
        this._dialog.invalidate()
    }

    _onEditSave(jobDefinitionsPath, jobDefinitions) {
        let order = this._model.getProperty("/newJob/job/order");
        let jobs = [];
        let newJob = this._model.getProperty("/newJob/job");
        jobDefinitions.forEach(function (e) {
            if (e.order === order) {
                jobs.push(newJob)
            } else {
                jobs.push(e)
            }
        });
        this._model.setProperty(jobDefinitionsPath, jobs);
    }

    _onCreateSave(jobDefinitionsPath, jobDefinitions) {
        if (!this._model.getProperty("/workflow/dagDefinitionJoined")) {
            this._model.setProperty("/workflow/dagDefinitionJoined", {
                jobDefinitions: []
            });
        }
        if(!jobDefinitions) jobDefinitions = [];
        jobDefinitions.forEach((element, index) => element.order = index);
        this._model.getProperty("/newJob/job").order = jobDefinitions.length;
        jobDefinitions.push(this._model.getProperty("/newJob/job"));
        this._model.setProperty(jobDefinitionsPath, jobDefinitions);
    }

    _prepareJobData() {
        if (this._model.getProperty("/newJob/isEdit")) {
            let order = this._model.getProperty("/newJob/order");
            let old = this._model.getProperty("/workflow/dagDefinitionJoined/jobDefinitions/"+order);
            this._model.setProperty("/newJob/job", {...$.extend(true, {}, old)})
        } else {
            this._model.setProperty("/newJob/job", {...$.extend(true, {}, this._emptyJob)});
        }
    }

    onClosePress() {
        this._dialog.close();
    }

    jobTypes = [
        {name: "Spark", fragment: "spark", controller: "SparkController"},
        {name: "Shell", fragment: "shell", controller: "ShellController"}
    ]

    _emptyJobParameters = {
        variables: {
            deploymentMode: "cluster"
        },
        maps: {}
    }

    _emptyJob = {
        jobType: {
            name: "Spark"
        },
        jobParameters: {
            variables: {
                deploymentMode: "cluster"
            },
            maps: {}
        },
        dagDefinitionJoined: {
            jobDefinitions: []
        }
    }

}
