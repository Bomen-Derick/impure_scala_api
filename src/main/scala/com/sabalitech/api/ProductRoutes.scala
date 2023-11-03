package com.sabalitech.api

import akka.http.scaladsl.server.Directives._
import com.sabalitech.db.Repository
import com.sabalitech.models.{Product, ProductId}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import eu.timepit.refined.auto._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Bomen Derick.
  */

final class ProductRoutes(repo: Repository)(implicit ec: ExecutionContext) {
  val routes = path("product" / JavaUUID) { id: ProductId =>
    get {
      rejectEmptyResponse {
        complete {
          for {
            rows <- repo.loadProduct(id)
            prod <- Future { Product.fromDatabase(rows) }
          } yield prod
        }
      }
    } ~ put {
      entity(as[Product]) { p =>
        complete {
          repo.updateProduct(p)
        }
      }
    }
  }
}
