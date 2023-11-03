

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import com.sabalitech.api.{ProductRoutes, ProductsRoutes}
import com.sabalitech.db.Repository
import eu.timepit.refined.auto._
import org.flywaydb.core.Flyway
import slick.basic._
import slick.jdbc._

import scala.io.StdIn
import scala.concurrent.ExecutionContext

/**
 * Created by Bomen Derick
 */

object Server {
  /**
   * Main entry point of the application.
   *
   * @param args A list of arguments given on the command line.
   */
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    val url = "jdbc:postgresql://" +
      system.settings.config.getString("database.db.properties.serverName") +
      ":" + system.settings.config.getString("database.db.properties.portNumber") +
      "/" + system.settings.config.getString("database.db.properties.databaseName")
    val user = system.settings.config.getString("database.db.properties.user")
    val pass = system.settings.config.getString("database.db.properties.password")
    val flyway: Flyway = Flyway.configure().dataSource(url, user, pass).load()
    val _ = flyway.migrate()

    val dbConfig: DatabaseConfig[JdbcProfile] =
      DatabaseConfig.forConfig("database", system.settings.config)
    val repo = new Repository(dbConfig)

    val productRoutes = new ProductRoutes(repo)
    val productsRoutes = new ProductsRoutes(repo)
    val routes = productRoutes.routes ~ productsRoutes.routes

    val host = system.settings.config.getString("api.host")
    val port = system.settings.config.getInt("api.port")
    val srv = Http().bindAndHandle(routes, host, port)
    val pressEnter = StdIn.readLine()
    srv.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}