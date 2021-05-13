# Text Unit

> I guess what I really want to know is there 1 texture per texture unit? Or can I generate multiple textures in the same texture unit?

These two questions have nothing to do with each other, and the fact that you asked them both at the same time betrays a misunderstanding of how texturing works.

When you execute the following code:

```
GLuint tex;
glGenTextures(1, &tex);
```

You have (for all intents and purposes) created a texture. You have created a space that will hold all of the data associated with a texture. And the variable ‘tex’ now stores the reference to this texture.

When you call:

```
glActiveTexture(GL_TEXTURE0);
glBindTexture(GL_TEXTURE_2D, tex);
```

You are doing a number of things. You are attaching the texture object ‘tex’ to the texture unit number 0. You are telling OpenGL that the texture object ‘tex’ is a 2D texture, and you will refer to it (while it is bound) with GL_TEXTURE_2D.

When you call:

```
glTexImage(GL_TEXTURE_2D, ...);
```

You are telling OpenGL to create image data, associate that image data with the texture that is currently bound to the active texture unit (GL_TEXTURE0) and is bound to the GL_TEXTURE_2D target. When called immediately after our previous functions, that will refer to the texture object ‘tex’.

So this function will cause image data to be attached to the texture object ‘tex’. But only because it is the texture currently bound to the current active texture unit and the GL_TEXTURE_2D target.

Now, when you call:

```
glBindTexture(GL_TEXTURE_2D, 0);
```

You are telling OpenGL that the currently bound texture object (aka: ‘tex’) is no longer bound to the current texture unit and GL_TEXTURE_2D target. This means that if you call glTexImage again, it will **not affect** the texture object ‘tex’. Not unless you bind ‘tex’ again.

In short, ‘tex’ is a free-standing memory object. It stores all of the data associated with the texture. Unbinding it from a texture unit will have no affect on the **contents** of the object.

You can repeat this process for as long as you like. Well, until OpenGL runs out of memory. You can create textures with glGenTextures, bind them, create image data for them, and unbind them.

Now, when it comes time to **use** textures as source for image data, you must bind them again. But only the ones you need for that **particular** drawing operation.

Let’s say you have object A and object B. Object A needs texture ‘texA’ bound to texture unit 0, and object B needs textures ‘texB’ and ‘texC’, bound to texture units 0 and 1 respectively.

To render object A, you do this:

```
glActiveTexture(GL_TEXTURE0);
glBindTexture(GL_TEXTURE_2D, texA);
glEnable(GL_TEXTURE_2D); //Only use this if not using shaders.

glDrawElements(...);

//Cleanup texture binds.
glActiveTexture(GL_TEXTURE0);
glBindTexture(GL_TEXTURE_2D, 0);
glEnable(GL_TEXTURE_2D); //Only use this if not using shaders.
```

To render object B, you do this:

```
glActiveTexture(GL_TEXTURE0 + 0);
glBindTexture(GL_TEXTURE_2D, texB);
glEnable(GL_TEXTURE_2D); //Only use this if not using shaders.
glActiveTexture(GL_TEXTURE0 + 1);
glBindTexture(GL_TEXTURE_2D, texC);
glEnable(GL_TEXTURE_2D); //Only use this if not using shaders.

glDrawElements(...);

//Cleanup texture binds.
glActiveTexture(GL_TEXTURE0 + 0);
glBindTexture(GL_TEXTURE_2D, 0);
glEnable(GL_TEXTURE_2D); //Only use this if not using shaders.
glActiveTexture(GL_TEXTURE0 + 1);
glBindTexture(GL_TEXTURE_2D, 0);
glEnable(GL_TEXTURE_2D); //Only use this if not using shaders.
```

Does that answer your question? Even though binding a texture to a texture unit is necessary for creating it and for using it, that does not mean that texture units and texture objects are permanently associated with each other.

If we wanted, when rendering objectB, ‘texA’ could have been bound to texture unit 1 instead of ‘texC’.