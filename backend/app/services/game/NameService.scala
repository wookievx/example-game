package services.game

import com.example.model.OwnerId

import scala.collection.concurrent.TrieMap

class NameService {
  private val storage = TrieMap.empty[OwnerId, String]
  def save(id: OwnerId, name: String): Unit = storage.put(id, name)
  def get(id: OwnerId): Option[String] = storage.get(id)
  def remove(id: OwnerId): Unit = storage.remove(id)
}
