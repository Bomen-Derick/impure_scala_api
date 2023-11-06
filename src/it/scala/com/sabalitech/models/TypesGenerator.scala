package models

import cats.data._
import com.sabalitech.models._
import eu.timepit.refined.auto._
import eu.timepit.refined.api._
import org.scalacheck.{Arbitrary, Gen}

import java.util.UUID
import scala.collection.immutable._


/**
  * Created by Bomen Derick.
  */
object TypesGenerator {
  val DefaultProductName: ProductName = "I am a product name!"

  val genLanguageCode: Gen[LanguageCode] = Gen.oneOf(LanguageCodes.all)

  val genUuid: Gen[UUID] = Gen.delay(UUID.randomUUID)

  val genProductId: Gen[ProductId] = genUuid

  val genProductName: Gen[ProductName] = for {
    cs <- Gen.nonEmptyListOf(Gen.alphaNumChar)
    name = RefType.applyRef[ProductName](cs.mkString).getOrElse(DefaultProductName)
  } yield name

  val genTranslation: Gen[Translation] = for {
    c <- genLanguageCode
    n <- genProductName
  } yield
    Translation(
      lang = c,
      name = n
    )

  implicit val arbitraryTranslation: Arbitrary[Translation] = Arbitrary(genTranslation)

  val genTranslationList: Gen[List[Translation]] = for {
    ts <- Gen.nonEmptyListOf(genTranslation)
  } yield ts

  val genNonEmptyTranslationList: Gen[NonEmptySet[Translation]] = for {
    t <- genTranslation
    ts <- genTranslationList
    ns = NonEmptyList.fromList(ts).map(_.toNes)
  } yield ns.getOrElse(NonEmptySet.one(t))

  val genProduct: Gen[Product] = for {
    id <- genProductId
    ts <- genNonEmptyTranslationList
  } yield
    Product(
      id = id,
      names = ts
    )

  implicit val arbitraryProduct: Arbitrary[Product] = Arbitrary(genProduct)

  val genProducts: Gen[List[Product]] = Gen.nonEmptyListOf(genProduct)

  implicit val arbitraryProducts: Arbitrary[List[Product]] = Arbitrary(genProducts)

}
