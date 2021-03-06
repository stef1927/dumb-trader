package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import models._
import services.ProductDao
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import java.util.Date
import scala.util.{Success, Failure}

object ProductController extends Controller {

  def getProducts(page: Int, perPage: Int) = Action { implicit req =>
    Async {
      for {
        count <- ProductDao.count
        products <- ProductDao.findAll(page, perPage)
      } yield {
        val result = Ok(Json.toJson(products))

        // Calculate paging headers, if necessary
        val next = if (count > (page + 1) * perPage) Some("next" -> (page + 1)) else None
        val prev = if (page > 0) Some("prev" -> (page - 1)) else None
        val links = next ++ prev
        if (links.isEmpty) {
          result
        } else {
          result.withHeaders("Link" -> links.map {
            case (rel, p) =>
              "<" + routes.ProductController.getProducts(p, perPage).absoluteURL() + ">; rel=\"" + rel + "\""
          }.mkString(", "))
        }
      }
    }
  }
  
    case class ProductForm(code: String, description: String) {
      def toProduct: Product = {
        val now = new Date
        Product(BSONObjectID.generate, code.toUpperCase, description, 0, now)
      }
  }

  implicit val productFormFormat = Json.format[ProductForm]
  
  def saveProduct = Action(parse.json) { req =>
    Logger.info("Save product " + req.body)
    Json.fromJson[ProductForm](req.body).fold(
      invalid => BadRequest("Bad product data"),
      form => Async {
        ProductDao.save(form.toProduct).map(_ => Created) .recover { 
          case e => BadRequest(e.getMessage) 
        }
      }
    )
  }
  
  def deleteProduct = Action(parse.json) { req =>
    Logger.info("Delete product " + req.body)
    Json.fromJson[Product](req.body).fold(
      invalid => BadRequest("Bad product data"),
      product => Async {
        ProductDao.delete(product).map(_ => Ok).recover{ 
          case e => BadRequest(e.getMessage) 
        }
      }
    )
  }

}