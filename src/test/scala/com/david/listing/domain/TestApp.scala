package com.david.listing.domain

object  TestApp  extends App {

  def mix(item: Char, items: List[Char]): List[List[Char]]={
    (0 to items.size).toList.foldLeft(List[List[Char]]()){case (agg, i) =>
      val aa = ( items.take(i) ::: item :: items.drop(i) )
      aa :: agg
    }
  }
  def permutations(input: List[Char]):List[List[Char]]= {
    val array = input.toArray
    (0 to input.size-1).foldLeft(List[List[Char]]()){ case (agg, i) =>
      val removedItem = input.take(i) ::: input.drop(i+1)
      val perm = permutations(removedItem)
      val mixed =perm match {
        case Nil => List(List(array(i)))
        case permutt => permutt.map(mix(array(i), _)).flatten
      }

      agg ::: mixed
    }
  }

  println(permutations(List('a', 'b','c')).toSet)
}
