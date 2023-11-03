package com.sabalitech.api

import akka.NotUsed
import akka.http.scaladsl.common._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import cats.implicits._
import com.sabalitech.db.Repository
import com.sabalitech.models.Product
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import eu.timepit.refined.auto._

import scala.concurrent.ExecutionContext
/**
  * Created by Bomen Derick.
  */
final class ProductsRoutes(repo: Repository)(implicit ec: ExecutionContext) {
  val routes = path("products") {
    get {
      implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
        EntityStreamingSupport.json()

      val src = Source.fromPublisher(repo.loadProducts())
      val products: Source[Product, NotUsed] = src
        .collect(
          cs =>
            Product.fromDatabase(Seq(cs)) match {
              case Some(p) => p
            }
        )
        .groupBy(Int.MaxValue, _.id)
        .fold(Option.empty[Product])(
          (op, x) => op.fold(x.some)(p => p.copy(names = p.names ++ x.names).some)
        )
        .mergeSubstreams
        .collect {
          case Some(p) => p
        }
      complete(products)
    } ~
      post {
        entity(as[Product]) { p =>
          complete {
            repo.saveProduct(p)
          }
        }
      }
  }
}
