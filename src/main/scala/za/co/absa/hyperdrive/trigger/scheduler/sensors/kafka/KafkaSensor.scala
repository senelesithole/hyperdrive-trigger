package za.co.absa.hyperdrive.trigger.scheduler.sensors.kafka

import java.time.Duration
import java.util.Collections

import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import play.api.libs.json.Json
import za.co.absa.hyperdrive.trigger.models.{Event, Properties}
import za.co.absa.hyperdrive.trigger.scheduler.sensors.PollSensor
import za.co.absa.hyperdrive.trigger.scheduler.utilities.KafkaConfig

import scala.concurrent.{ExecutionContext, Future}
import za.co.absa.hyperdrive.trigger.scheduler.utilities.KafkaRichConsumer._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class KafkaSensor(
  eventsProcessor: (Seq[Event], Properties) => Future[Boolean],
  properties: Properties,
  executionContext: ExecutionContext
) extends PollSensor(eventsProcessor, properties, executionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val logMsgPrefix = s"Sensor id = ${properties.sensorId}."

  private val kafkaSettings = KafkaSettings(properties.settings)

  private val consumer = new KafkaConsumer[String, String](KafkaConfig.getConsumerProperties(kafkaSettings))

  try {
    consumer.subscribe(Collections.singletonList(kafkaSettings.topic))
  } catch {
    case e: Exception => logger.debug(s"$logMsgPrefix. Exception during subscribe.", e)
  }

  override def poll(): Future[Unit] = {
    logger.debug(s"$logMsgPrefix. Pooling new events.")
    val fut = Future {
      consumer.pollAsScala(Duration.ofMillis(KafkaConfig.getPollDuration))
    } flatMap processRecords map (_ => consumer.commitSync())

    fut.onComplete {
      case Success(_) => logger.debug(s"$logMsgPrefix. Pooling successful")
      case Failure(exception) => {
        logger.debug(s"$logMsgPrefix. Pooling failed.", exception)
      }
    }

    fut
  }

  private def processRecords[A](records: Iterable[ConsumerRecord[A, String]]): Future[Unit] = {
    logger.debug(s"$logMsgPrefix. Messages received = ${records.map(_.value())}")
    if(records.nonEmpty)
      eventsProcessor.apply(records.map(recordToEvent).toSeq, properties).map(_ => ():Unit)
    else
      Future.successful(():Unit)
  }

  private def recordToEvent[A](record: ConsumerRecord[A, String]): Event = {
    val sourceEventId = properties.sensorId + "kafka" + record.topic() + record.partition() + record.offset()
    val payload = Try(
      Json.parse(record.value())
    ).getOrElse {
      logger.debug(s"$logMsgPrefix. Invalid message.")
      Json.parse(s"""{"errorMessage": "${record.value()}"}""")
    }
    Event(sourceEventId, properties.sensorId, payload)
  }

  override def close(): Unit = {
    consumer.unsubscribe()
  }

}