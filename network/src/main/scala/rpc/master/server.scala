package network.rpc.master.server

import io.grpc.{Server, ServerBuilder};
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.concurrent.{ExecutionContext, Future}
import com.google.protobuf.ByteString

import protos.distsortMaster.{
  DistsortMasterGrpc, 
  ReadyRequest,
  ReadyReply,
  KeyRangeRequest,
  KeyRangeReply,
  PartitionCompleteRequest,
  PartitionCompleteReply,
  ExchangeCompleteRequest,
  ExchangeCompleteReply,
  SortFinishRequest,
  SortFinishReply
}

abstract class DistSortServer(port: Int, executionContext: ExecutionContext) {
  private val logger = Logger.getLogger(classOf[DistSortServer].getName)
  private var server: Server = null

  def start(): Unit = {
    server = ServerBuilder.forPort(port).addService(DistsortMasterGrpc.bindService(new DistsortImpl, executionContext)).build.start
    this.logger.info("Server started, listening on " + port)
    sys.addShutdownHook {
      logger.info("*** shutting down gRPC server since JVM is shutting down")
      this.stop()
      logger.info("*** server shut down")
    }
  }

  def getListenAddress(): String = {
    val socket = server.getListenSockets().get(0)
    socket.toString()
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  def handleReadyRequest(workerName: String, workerIpAddress: String): Unit

  def handleKeyRangeRequest(
      populationSize: Int,
      numSamples: Int,
      samples: Iterable[Iterable[Byte]],
    ): (String, (List[Byte], List[Byte]))

  def handlePartitionRequest(): Unit

  def handleSortFinishRequest(): Unit

  private class DistsortImpl extends DistsortMasterGrpc.DistsortMaster {
    override def workerReady(req: ReadyRequest): Future[ReadyReply] = {
      logger.info("Received ready request from " + req.workerName)

      val _ = handleReadyRequest(req.workerName, req.workerIpAddress)
      val reply = ReadyReply()
      Future.successful(reply)
    }

    override def keyRange(req: KeyRangeRequest): Future[KeyRangeReply] = {
      logger.info("Received KeyRange request")

      val numSamples = req.numSamples;
      val samples = req.samples;

      val sampleString = samples.foldRight(List[Array[Byte]]()){ (x, acc) => x.toByteArray::acc}
      logger.info("Reveived " + numSamples + " samples from worker");

      val reply = KeyRangeReply(
        keyList = List(),
        workerIpList = List(),
      )
      Future.successful(reply)
    }

    override def exchangeComplete(request: ExchangeCompleteRequest): Future[ExchangeCompleteReply] = {
      // Todo: implement this
      val reply = ExchangeCompleteReply()
      Future.successful(reply)
    }

    override def partitionComplete(request: PartitionCompleteRequest): Future[PartitionCompleteReply] = {
      // Todo: implement this
      val reply = PartitionCompleteReply()
      Future.successful(reply)
    }

    override def sortFinish(req: SortFinishRequest): Future[SortFinishReply] = {
      logger.info("Received sortFinish request")

      val _ = handleSortFinishRequest()
      val reply = SortFinishReply()
      Future.successful(reply)
    }
  }
}