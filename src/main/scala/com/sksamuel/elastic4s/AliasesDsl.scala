package com.sksamuel.elastic4s

import org.elasticsearch.action.admin.indices.alias.get.{ GetAliasesRequest, GetAliasesResponse }
import org.elasticsearch.action.admin.indices.alias.{ IndicesAliasesRequest, IndicesAliasesResponse }
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.metadata.AliasAction
import org.elasticsearch.index.query.FilterBuilder

import scala.concurrent.Future

trait AliasesDsl {

  def aliases(aliasMutations: MutateAliasDefinition*) = new IndicesAliasesRequestDefinition(aliasMutations: _*)

  class AddAliasExpectsIndex(alias: String) {
    def on(index: String) = new MutateAliasDefinition(new AliasAction(AliasAction.Type.ADD, index, alias))
  }

  class RemoveAliasExpectsIndex(alias: String) {
    def on(index: String) = new MutateAliasDefinition(new AliasAction(AliasAction.Type.REMOVE, index, alias))
  }

  implicit object GetAliasDefinitionExecutable
      extends Executable[GetAliasDefinition, GetAliasesResponse] {
    override def apply(c: Client, t: GetAliasDefinition): Future[GetAliasesResponse] = {
      injectFuture(c.admin.indices.getAliases(t.build, _))
    }
  }

  implicit object MutateAliasDefinitionExecutable
      extends Executable[MutateAliasDefinition, IndicesAliasesResponse] {
    override def apply(c: Client, t: MutateAliasDefinition): Future[IndicesAliasesResponse] = {
      injectFuture(c.admin.indices.aliases(t.build, _))
    }
  }

  implicit object IndicesAliasesRequestDefinitionExecutable
      extends Executable[IndicesAliasesRequestDefinition, IndicesAliasesResponse] {
    override def apply(c: Client, t: IndicesAliasesRequestDefinition): Future[IndicesAliasesResponse] = {
      injectFuture(c.admin.indices.aliases(t.build, _))
    }
  }
}

class GetAliasDefinition(aliases: Seq[String]) {
  val request = new GetAliasesRequest(aliases.toArray)
  def build = request
  def on(indexes: String*): GetAliasDefinition = {
    request.indices(indexes: _*)
    this
  }
}

class MutateAliasDefinition(val aliasAction: AliasAction) {
  def routing(route: String) = new MutateAliasDefinition(aliasAction.routing(route))
  def filter(filter: FilterBuilder) = new MutateAliasDefinition(aliasAction.filter(filter))
  def build = new IndicesAliasesRequest().addAliasAction(aliasAction)
}

class IndicesAliasesRequestDefinition(aliasMutations: MutateAliasDefinition*) {
  def build = aliasMutations.foldLeft(new IndicesAliasesRequest())(
    (request, aliasDef) => request.addAliasAction(aliasDef.aliasAction))
}

object Main extends App {

  import com.sksamuel.elastic4s.ElasticDsl._

  val client = ElasticClient.local
  client.execute(create index "bands")
  client.execute {
    index into "bands/artists" fields "name" -> "coldplay"
  }.await
  val resp = client.execute {
    search in "bands/artists" query "coldplay"
  }.await
  println(resp)
  client.shutdown
  client.close()
}
