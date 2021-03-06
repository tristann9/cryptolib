/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cryptolib.v1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.FileContentCryptor;
import org.cryptomator.cryptolib.api.FileHeader;
import org.cryptomator.cryptolib.api.FileHeaderCryptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class EncryptingWritableByteChannelTest {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private ByteBuffer dstFile;
	private WritableByteChannel dstFileChannel;
	private Cryptor cryptor;
	private FileContentCryptor contentCryptor;
	private FileHeaderCryptor headerCryptor;
	private FileHeader header;

	@Before
	public void setup() {
		dstFile = ByteBuffer.allocate(100);
		dstFileChannel = new SeekableByteChannelMock(dstFile);
		cryptor = Mockito.mock(Cryptor.class);
		contentCryptor = Mockito.mock(FileContentCryptor.class);
		headerCryptor = Mockito.mock(FileHeaderCryptor.class);
		header = Mockito.mock(FileHeader.class);
		Mockito.when(cryptor.fileContentCryptor()).thenReturn(contentCryptor);
		Mockito.when(cryptor.fileHeaderCryptor()).thenReturn(headerCryptor);
		Mockito.when(contentCryptor.cleartextChunkSize()).thenReturn(10);
		Mockito.when(headerCryptor.create()).thenReturn(header);
		Mockito.when(headerCryptor.encryptHeader(header)).thenReturn(ByteBuffer.wrap("hhhhh".getBytes()));
		Mockito.when(contentCryptor.encryptChunk(Mockito.any(ByteBuffer.class), Mockito.anyLong(), Mockito.any(FileHeader.class))).thenAnswer(new Answer<ByteBuffer>() {

			@Override
			public ByteBuffer answer(InvocationOnMock invocation) throws Throwable {
				ByteBuffer input = invocation.getArgumentAt(0, ByteBuffer.class);
				String inStr = UTF_8.decode(input).toString();
				return ByteBuffer.wrap(inStr.toUpperCase().getBytes(UTF_8));
			}

		});
	}

	@Test
	public void testEncryption() throws IOException {
		try (EncryptingWritableByteChannel ch = new EncryptingWritableByteChannel(dstFileChannel, cryptor)) {
			ch.write(UTF_8.encode("hello world 1"));
			ch.write(UTF_8.encode("hello world 2"));
		}
		dstFile.flip();
		Assert.assertArrayEquals("hhhhhHELLO WORLD 1HELLO WORLD 2".getBytes(), Arrays.copyOfRange(dstFile.array(), 0, dstFile.remaining()));
	}

}
