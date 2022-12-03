package master.server

import network.rpc.master.server.DistSortServer
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import scala.concurrent.{Promise, SyncVar, Future, Await, ExecutionContext, blocking}
import scala.concurrent.duration.Duration
import master.util.sync.SyncAccList
import master.Master


object DistSortServerImpl {
  final val port = 55555
  implicit val ec = ExecutionContext.global

  private def printConnectedWorkers(workers: List[String]) = {
    println(workers.mkString(", "))
  }
  
  def serveRPC(numWorkers: Int) = {
    val connectedWorkers = Promise[List[String]]
    val server = new DistSortServerImpl(port, numWorkers, connectedWorkers)
    server.start()
    val localIpAddress = InetAddress.getLocalHost.getHostAddress
    println(localIpAddress)
    connectedWorkers.future.foreach(printConnectedWorkers)
    server.blockUntilShutdown()
  }
}

class DistSortServerImpl(port: Int, numWorkers: Int, connectedWorkers: Promise[List[String]])
extends DistSortServer(port, ExecutionContext.global) {
  implicit private val ec = ExecutionContext.global

  private val master = new Master(numWorkers)

  private val readyRequestLatch = new CountDownLatch(numWorkers)
  private val keyRangeRequestLatch = new CountDownLatch(numWorkers)
  private val partitionRequestLatch = new CountDownLatch(numWorkers)
  private val shutdownLatch = new CountDownLatch(numWorkers)

  private val syncConnectedWorkers = new SyncAccList[String](List())

  private val triggerShutdown = Promise[Unit]
  triggerShutdown.future.foreach{_ => Future { blocking {
      Thread.sleep(3000)
      this.stop()
    }
  }}

  def handleReadyRequest(workerName: String, workerIpAddress: String) = {
    syncConnectedWorkers.accumulate(List(workerIpAddress))
    readyRequestLatch.countDown()
    readyRequestLatch.await()
    connectedWorkers.trySuccess(syncConnectedWorkers.get)
  }

  def handleKeyRangeRequest(
    numSamples: Int,
    samples: List[Array[Byte]],
  ): (List[Array[Byte]], List[String]) = {
    val keyRangeResult = master.divideKeyRange(numSamples, samples)
    keyRangeRequestLatch.countDown()
    keyRangeRequestLatch.await()
    keyRangeResult.get
  }

  def handlePartitionRequest() = {
    partitionRequestLatch.countDown()
    partitionRequestLatch.await()
  }

  def handleSortFinishRequest() = {
    shutdownLatch.countDown()
    shutdownLatch.await()
    triggerShutdown.trySuccess(())
  }
}