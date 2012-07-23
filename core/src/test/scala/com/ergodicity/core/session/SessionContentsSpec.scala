package com.ergodicity.core.session

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.event.Logging
import akka.pattern.ask
import akka.util.duration._
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.FSM.SubscribeTransitionCallBack
import com.ergodicity.core.AkkaConfigurations.ConfigWithDetailedLogging
import com.ergodicity.core.common.{Isin, FutureContract}
import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.Await
import akka.util.Timeout
import com.ergodicity.cgate.scheme.FutInfo
import com.ergodicity.cgate.repository.Repository.Snapshot
import com.ergodicity.core.Mocking._


class SessionContentsSpec extends TestKit(ActorSystem("SessionContentsSpec", ConfigWithDetailedLogging)) with ImplicitSender with WordSpec with BeforeAndAfterAll {
  val log = Logging(system, self)

  override def afterAll() {
    system.shutdown()
  }

  implicit val timeout = Timeout(1.second)

  val gmkFuture = mockFuture(4023, 166911, "GMKR-6.12", "GMM2", "Фьючерсный контракт GMKR-06.12", 115, 2)

  "StatefulSessionContents" must {

    "return None if no instument found" in {
      val contents = TestActorRef(new StatefulSessionContents[FutureContract, FutInfo.fut_sess_contents](SessionState.Online), "Futures")
      val future = (contents ? GetSessionInstrument(Isin(100, "BadCode", "BadShortCode"))).mapTo[Option[ActorRef]]
      assert(Await.result(future, 1.second) == None)
    }

    "return instument reference if found" in {
      val contents = TestActorRef(new StatefulSessionContents[FutureContract, FutInfo.fut_sess_contents](SessionState.Online), "Futures")
      contents ! TrackSessionState(self)
      expectMsgType[SubscribeTransitionCallBack]
      contents ! Snapshot(self, gmkFuture :: Nil)
      Thread.sleep(1000)
      val gmk = system.actorFor("user/Futures/GMKR-6.12")
      val future = (contents ? GetSessionInstrument(Isin(166911, "GMKR-6.12", "GMM2"))).mapTo[Option[ActorRef]]
      assert(Await.result(future, 1.second) == Some(gmk))
    }
  }
}