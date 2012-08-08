Android-Asynchronous-Collection

A class to deserialise a JSON string array of objects into a collection of objects.

This was built for use on the Android (2.3.3) platform, however could be used in a any Java implementation.

Requires:
- Google JSON library

Todo:
- Improve error reporting
- Support singular object deserialisation

Usage:

To use, implement the CollectionResult interface. This has three methods:
1. Start - Before request sent - generally used for showing a loader
2. Get Result - Request finished with data - populate your view
3. End - Request finished - hide loader and tidy up

Create a class to deserialise to. This must have public variables with the same name as in the JSON string with relevant data types.

--------------------------

```java
CollectionResult<Review> collectionResult = new CollectionResult<Review>(){
	ProgressDialog loadingDialog = null;
    
	@Override
	public void getResult(ArrayList<Review> prods) {				

		listViewArrayAdapter = new ReviewAdapter(c, R.layout.review_cell, prods);
        list.setAdapter(listViewArrayAdapter);
        
        listViewArrayAdapter.notifyDataSetChanged();
        
    }
	
	@Override
	public void start(){
		loadingDialog = ProgressDialog.show(c, "", "Loading. Please wait...", true);
	}
	
	@Override
	public void end(){
		loadingDialog.dismiss();
	}
};

pcol = new AsyncCollection<Review>(collectionResult, Review.class, "http://www.example.com/reviews");
pcol.tabActivityGroup = parentTabContainer;
pcol.addParam("id", "1");
pcol.execute();```

----------------------