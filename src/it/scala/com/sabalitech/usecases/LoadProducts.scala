package com.sabalitech.usecases

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString
import cats.implicits._
import com.sabalitech.models._
import com.sabalitech.models.TypesGenerator._
import io.circe.parser._

import scala.collection.immutable._
import scala.concurrent.Future

/**
  * Created by Bomen Derick.
  */
class LoadProducts extends BaseUseCaseSpec {
  private final val http = Http()

  /**
   * Before each test we clean and migrate the database.
   */
  override protected def beforeEach(): Unit = {
    flyway.clean()
    val _ = flyway.migrate()
    super.beforeEach()
  }

  /**
   * After each test we clean the database again.
   */
  override protected def afterEach(): Unit = {
    flyway.clean()
    super.afterEach()
  }

  "Loading all products" when {
    "no products exist" must {
      val expectedStatus = StatusCodes.OK

      s"return $expectedStatus and an empty list" in {
        for {
          resp <- http.singleRequest(
            HttpRequest(
              method = HttpMethods.GET,
              uri = s"$baseUrl/products",
              headers = Seq(),
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                data = ByteString("")
              )
            )
          )
          body <- resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
        } yield {
          resp.status must be(expectedStatus)
          decode[List[Product]](body.utf8String) match {
            case Left(e) => fail(s"Could not decode response: $e")
            case Right(d) => d must be(empty)
          }
        }
      }
    }

    "products exist" must {
      val expectedStatus = StatusCodes.OK

      s"return $expectedStatus and a list with all products" in {
        genProducts.sample match {
          case None => fail("Could not generate data sample!")
          case Some(ps) =>
            for {
              _ <- Future.sequence(ps.map(p => repo.saveProduct(p)))
              resp <- http.singleRequest(
                HttpRequest(
                  method = HttpMethods.GET,
                  uri = s"$baseUrl/products",
                  headers = Seq(),
                  entity = HttpEntity(
                    contentType = ContentTypes.`application/json`,
                    data = ByteString("")
                  )
                )
              )
              body <- resp.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
            } yield {
              resp.status must be(expectedStatus)
              decode[List[Product]](body.utf8String) match {
                case Left(e) => fail(s"Could not decode response: $e")
                case Right(d) => d.sorted mustEqual ps.sorted
              }
            }
        }
      }
    }
  }
}
