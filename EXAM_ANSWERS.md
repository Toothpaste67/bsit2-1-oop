# LICEO RIDE - Semi-Final Laboratory Exam Answers
Name: Ioan James Enghog

Section: BSIT 2-1

## Question 1
Why is the list in RideManager typed ArrayList&lt;Ride&gt; and not ArrayList&lt;Jeepney&gt;?
Your answer: The list is ArrayList<Ride> because it can store different types of rides, such as Jeepney, since they are all considered Ride objects.

## Question 2
In showStudentDiscounts(), why must you check instanceof before the cast?
Your answer: You must check instanceof before the cast to make sure the Ride object actually supports StudentDiscount. Otherwise, the cast could cause an error if the ride does not implement StudentDiscount.
