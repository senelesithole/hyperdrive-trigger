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

package za.co.absa.hyperdrive.trigger.models.enums

object JobStatuses {

  sealed abstract class JobStatus(val name: String, val isFinalStatus: Boolean, val isFailed: Boolean, val isRunning: Boolean) {
    override def toString: String = name
  }

  case object InQueue extends JobStatus("InQueue", false, false, false)
  case object Submitting extends JobStatus("Submitting", false, false, true)
  case object SubmitFail extends JobStatus("SubmitFail", true, true, false)
  case object Running extends JobStatus("Running", false, false, true)
  case object Lost extends JobStatus("Lost", true, true, false)
  case object Succeeded extends JobStatus("Succeeded", true, false, false)
  case object Failed extends JobStatus("Failed", true, true, false)
  case object Killed extends JobStatus("Killed", true, true, false)
  case object InvalidExecutor extends JobStatus("InvalidExecutor", true, true, false)
  case object FailedPreviousJob extends JobStatus("FailedPreviousJob", true, true, false)

  val statuses: Set[JobStatus] = Set(InQueue,Submitting,SubmitFail,Running,Lost,Succeeded,Failed,Killed,InvalidExecutor,FailedPreviousJob)
  val finalStatuses: Set[JobStatus] = statuses.filter(!_.isFinalStatus)
  val nonFinalStatuses: Set[JobStatus] = statuses.filter(_.isFinalStatus)
}