package za.co.absa.hyperdrive.trigger.persistance

import java.time.LocalDateTime

import slick.dbio.{DBIOAction, Effect, NoStream}
import za.co.absa.hyperdrive.trigger.models.JobInstance
import slick.dbio.DBIO
import za.co.absa.hyperdrive.trigger.models.tables.JdbcTypeMapper._
import za.co.absa.hyperdrive.trigger.models.enums.JobStatuses
import za.co.absa.hyperdrive.trigger.models.enums.JobStatuses.JobStatus
import za.co.absa.hyperdrive.trigger.models.tables.JDBCProfile.profile._

import scala.concurrent.{ExecutionContext, Future}

trait JobInstanceRepository extends Repository {
  def insertJobInstances(jobs: Seq[JobInstance], transactionDBIO: DBIOAction[Unit, NoStream, Effect.Write])(implicit ec: ExecutionContext): Future[Unit]
  def updateJob(job: JobInstance)(implicit ec: ExecutionContext): Future[Unit]
  def updateJobsStatus(ids: Seq[Long], status: JobStatus)(implicit ec: ExecutionContext): Future[Unit]
  def getNewActiveJobs(jobsIdToFilter: Seq[Long], size: Int)(implicit ec: ExecutionContext): Future[Seq[JobInstance]]
  def getJobInstances(dagInstanceId: Long)(implicit ec: ExecutionContext): Future[Seq[JobInstance]]
}

class JobInstanceRepositoryImpl extends JobInstanceRepository {

  override def insertJobInstances(
    jobs: Seq[JobInstance], transactionDBIO: DBIOAction[Unit, NoStream, Effect.Write]
  )(implicit ec: ExecutionContext): Future[Unit] = db.run {
    transactionDBIO andThen DBIO.seq(jobInstanceTable ++= jobs)
  }

  override def updateJob(job: JobInstance)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    jobInstanceTable.filter(_.id === job.id).update(job.copy(updated = Option(LocalDateTime.now()))).andThen(DBIO.successful((): Unit))
  }

  def updateJobsStatus(ids: Seq[Long], status: JobStatus)(implicit ec: ExecutionContext): Future[Unit] = db.run(
    jobInstanceTable.filter(_.id inSet ids).map(ji => (ji.jobStatus, ji.updated)).update((status, Option(LocalDateTime.now()))).transactionally
  ).map(_ => (): Unit)

  override def getNewActiveJobs(idsToFilter: Seq[Long], size: Int)(implicit ec: ExecutionContext): Future[Seq[JobInstance]] = db.run(
    jobInstanceTable.filter(
      ji =>
        !ji.id.inSet(idsToFilter) && ji.jobStatus.inSet(JobStatuses.finalStatuses)
    ).take(size).result
  )

  override def getJobInstances(dagInstanceId: Long)(implicit ec: ExecutionContext): Future[Seq[JobInstance]] = db.run(
    jobInstanceTable.filter(_.dagInstanceId === dagInstanceId).sortBy(_.id).result
  )

}