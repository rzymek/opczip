# Open Package Container ZIP64 streaming implementation
![build status](https://travis-ci.org/rzymek/opczip.svg?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.rzymek/opczip.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.rzymek%22%20AND%20a:%22opczip%22)

Drop in replacement for `java.util.ZipOutputStream`. Only Java ZIP64 implementation compatible with MS Excel.

### Usage
Replace usages of `java.util.ZipOutputStream` with `OpcOutputStream`.

Another option is to use `OpcZipOutputStream`. It extends `ZipOutputSteam` for compatibility, 
but replaces it's implementation with `OpcOutputStream`.   
 
## Problem with huge XLSX files

**TL;DR;** Excel requires specific ZIP flag values in `.xlsx` that Java's ZIP implementation
does not provide when streaming.

The standard in Excel file creation in Java is [Apache POI](http://poi.apache.org/). 
It works fine, a bit slow, but still fine. As it turns out up to some size limit, arbitrary 
at first sight. You can try it yourself. Just run this piece of code:

    try (SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook())) {
      SXSSFSheet sheet = wb.createSheet();
      for (int rowIdx = 0; rowIdx < 1_000_000; rowIdx++) {
        SXSSFRow row = sheet.createRow(rowIdx);
        for (int colIdx = 0; colIdx < 100; colIdx++) {
          row.createCell(colIdx).setCellValue(1000 * rowIdx + colIdx);
        }
      }
      try (OutputStream out = new FileOutputStream("big.xlsx")) {
        wb.write(out);
      }
    }

[Excel can handle](https://support.office.com/en-us/article/excel-specifications-and-limits-1672b34d-7043-467e-8e27-269d656771c3) up to 1,048,576 rows by 16,384 columns, so this is well within the limit. 

Try to open the resulting `big.xlsx` file in Excel. It will almost instantly 
pop up this dialog:

![excel](https://rzymek.github.io/img/excel-zip64/excel-repair.png)

Let it repair it. It will take a few minutes now. After that all the rows and cells will be there.
You can save it and it will open fine from now on.
So, what's the problem?

You may already know, xlsx files are just zip archived.
Unpack the repaired xlsx file. You'll find a bunch on XML files inside:
```
[Content_Types].xml
docProps/app.xml
docProps/core.xml
_rels/.rels
xl/workbook.xml
xl/_rels/workbook.xml.rels
xl/sharedStrings.xml
xl/styles.xml
xl/worksheets/sheet1.xml
```
Lets take those files and re-compress then with Java. Here's the code:

    List<String> paths = Arrays.asList(
      "[Content_Types].xml",
      "_rels/.rels",
      "docProps/app.xml",
      "docProps/core.xml",
      "xl/styles.xml",
      "xl/workbook.xml",
      "xl/_rels/workbook.xml.rels",
      "xl/worksheets/sheet1.xml"
    );
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream("repackaged.xlsx"))) {
        for (String path : paths) {
            out.putNextEntry(new ZipEntry(path));
            Files.copy(Paths.get("big",path), out);
            out.closeEntry();
        }
    }

Now it gets really interesting. The `repackaged.xlsx` file causes Excel to pop up the repair dialog as well!
It's not a problem with the XMLs or Apache POI. It's a problem with the ZIP format. Excel must have some
specific expectations, which `java.util.zip` does not fullfil. Obviously the files opens (and passes test) just 
fine in every other zip tool I tried. The list included standard Linux zip cli
 (v3.0, by Info-ZIP), 7z (v16.02 Linux and v17.01 Windows), Windows 10 Explorer.

![](https://rzymek.github.io/img/excel-zip64/test-ok.png)

I dug a bit deeper. Apache POI actually does not use the zip implementation provided with JDK. 
It uses [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/). 
A fully independent implementation. I'll spare you the code, but compressing the XMLs with it does not
solve the problem either. Excel wants to repair that one also.

So, I went down the rabbit whole. I took a handy [hex editor](http://www.wxhexeditor.org/), 
a copy of the [ZIP specification](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT) 
and went to implement my own ZIP format that *will* be consumed by Excel. 

## Possible source of the problem

Smaller xlsx files generated with Apache POI obviously open correctly in Excel. Well, duh. Thousands of 
developers are using it. It turns out not many of them are generating files that big. 
I found a few bugs in the Apache POI bugzilla that describe this exact issue. They are

* [54523](https://bz.apache.org/bugzilla/show_bug.cgi?id=54523) from **2013**-02-05 (!)
* [57342](https://bz.apache.org/bugzilla/show_bug.cgi?id=57342) from **2014**-12-11 (!)
* [61832](https://bz.apache.org/bugzilla/show_bug.cgi?id=61832) from 2017-11-29.

Both are unresolved. "Status: NEW".

The problem starts when any inner xml file's size exceeds 4GB. 
Let's see what this this particular size mark has to do with ZIP internals.

## 4GB in ZIP

Standard ZIP format has 4 bytes reserved for file size. The maximum is therefore `0xFFFF FFFF` or `256^4-1`, `2^32-1`. 
In other words (numbers) its `4_294_967_295`, that is 4GB minus 1 byte.  An unimaginably huge size in 1989 when the first PKZIP spec was published. Not that big now. The internal `sheet1.xml` file will break the `4GB` limit at about 1 million rows with 100 columns - with number cell only. Excel handles this amount of data surprisingly well. At least on by 32GB RAM machine.

As you might have guested, the `4GB` limit in ZIP files was overcome years ago. In 2001 actually, in the version 4.5 of the PKZIP specification. With the introduction of ZIP64 extension.

This is sufficient to handle file sizes up to `18_446_744_073_709_551_615` bytes or `2^64−1` bytes, 16 EiB minus 1 byte.

But is turns out Excel is quite strict when it comes to ZIP64 extension.

### Zip file structure

Let's first look a at standard zip file structure:
```
 +============+     
 | LFH-1      |   - Local file header for file 1
 +------------+
 | Compressed |   - Usually using deflate compression
 | file 1     |
 | data       |
 +------------+
 | EXT-1      |   - Data descriptor (optional), contains crc and file size
 +============+     
 | LFH-2      |   
 +------------+
 | Compressed |   
 | file 2     |
 | data       |
 +------------+
 | EXT-2      |   
 +============+     
 | LFH-3      |   
 +------------+
 | ...        |   
 |

              |
 |        ... |
 +------------+
 | EXT-n      |   
 +============+     
 | CEN-1      |   - Central directory entry, one for every file, 
 +------------+     points to each corresponding LFH offset
 | CEN-2      |
 +------------+
 | ...        | 
 +------------+
 | CEN-n      |
 +============+     
 | END        |   - End of zip header, points to CEN-1 offset
 +------------+
 ```

I'm going to focus on streaming zip creation. That is compressing data that is
generated on the fly and not known in advance. The output will also be streamed, like over a socket. 
No going back. No seeking to add some info to a LFH.

Zip does fully support this. There are field to store size, crc and compressed size in LFH, but they can be filled with zeros.

Let's look closely at a zip format Excel fully accepts.

### Local file header (LFH)

First the complete LFH - Local File Header:
```
 50 4B 03 04 2D 00 08 00 08 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ...
|-----------|-----|-----|-----|-----------|-----------|-----------|-----------|
 'PK\03\04'   |   flags   |   time & date    CRC-32        |     uncompressed  
 LFH          |           |                           compressed     size      
 signature    |     compression                         size                
              |       method                                                
           version                                                          
           2D = 45                                                          
```
```           
... 18 00 00 00 78 6C 2F 77 6F 72 6B 73 68 65 65 74 73 2F 73 68 65 65 74 31 2E 78 6D 6C 
   |-----|-----|-----------------------------------------------------------------------|
      |     |      filename ('xl/worksheets/sheet1.xml')
 filename   |      24 bytes (0x18) in this case
 length     |
          extra field length
```

Version `0x2D` = `45` is interpreted as 4.5. This is the ZIP specification version where ZIP64 extension was introduced.
This looks to be critically important to Excel. It will open files with zip version 2.0 just fine, as long as 
file size does not exceed 4GB. So, during streaming xlsx creation, if there's just a possibility that that the 32bit
limit could be exceeded, version 4.5 must be put here in the LFH. 
Flag value `0x0008` ([little-endian](https://en.wikipedia.org/wiki/Little-endian)) means that the bit at offset 3 is set. 
This bit is a marker that Data Descriptor (EXT) will be written after the file data. 
Compression method `0x0008` means [DEFLATE](https://en.wikipedia.org/wiki/DEFLATE). 

```
void writeLFH(ZipEntry entry) throws IOException {
  writeInt(0x04034b50L);                        // "PK\003\004"
  writeShort(45);                               // version required: 4.5
  writeShort(8);                                // flags: 8 = data descriptor used
  writeShort(8);                                // compression method: 8 = deflate
  writeInt(0);                                  // file modification time & date 
  writeInt(entry.crc);                          // CRC-32
  writeInt(0);                                  // compressed file size
  writeInt(0);                                  // uncompressed file size
  writeShort(entry.filename.length());          // filename length
  writeShort(0);                                // extra flags size
  out.write(entry.filename.getBytes(US_ASCII)); // filename characters
}
```

After the header comes actual compressed file data. When reimplementing the zip format (to match Excel expectations) 
I just used [java.util.zip.DeflaterOutputStream](https://docs.oracle.com/javase/8/docs/api/java/util/zip/DeflaterOutputStream.html). Actually I did extended it to add functionality to record CRC and to wrap it in `BufferedOutputStream`. Turns out if you don't feed `DeflaterOutputStream` in chunks of about `4096` bytes it becomes really slow. Like *three times slower*. I did stumble upon this observation while browsing through 
[`commons-compress` sources](https://github.com/apache/commons-compress/blob/c03704d773dfa0dfc5b2e53b4c198a95d0213ca0/src/main/java/org/apache/commons/compress/archivers/zip/StreamCompressor.java#L42):

    /*
     * Apparently Deflater.setInput gets slowed down a lot on Sun JVMs
     * when it gets handed a really big buffer.  See
     * https://issues.apache.org/bugzilla/show_bug.cgi?id=45396
     *
     * Using a buffer size of 8 kB proved to be a good compromise
     */

### Data descriptor

Getting back to ZIP structure. After compressed file data comes the optional *Data Descriptor* (EXT) header. It contains CRC, size and compressed size. Initially 4 bytes were reserved for each of these values. ZIP specification 4.5 is used, the compressed and uncompressed sizes are 8 bytes each.
```
 50 4B 07 08 73 B9 D9 10 06 66 30 21 00 00 00 00 9A 90 DC 15 01 00 00 00
|-----------|-----------|-----------------------|-----------------------|
 'PK\07\08'    CRC-32    compressed size          uncompressed size
                          0x21 30 66 06 =          0x01 15 DC 90 9A =
                          556819974 bytes (532mb)  4661743770 bytes (4,4GiB)
```
```
void writeEXT(ZipEntry entry) throws IOException {
  writeInt(0x08074b50L);           // data descriptor signature "PK\007\008"
  writeInt(entry.crc);             // crc-32
  writeLong(entry.compressedSize); // compressed size (zip64)
  writeLong(entry.size);           // uncompressed size (zip64)
}
```

This is repeated for every file.


### Central directory

After all the files comes the *Central Directory*. Here for every file comes a structure very similar to local file header. 
Seems Excel really focuses on LFH and is not that strict with central directory. Nevertheless here's an example and implementation
that will just work.

```
 50 4B 01 02 2D 00 2D 00 08 00 08 00 00 00 00 00 73 B9 D9 10 06 66 30 21 FF FF FF FF ...
|-----------|-----|-----|-----|-----|-----------|-----------|-----------|-----------| 
 'PK\01\02'    |     |      |    |    time & date    CRC-32        |     uncompressed  
         version  version   | compression                     compressed     size      
         made by  required  |                                    size                
                          flag  
```
```
... 18 00 0C 00 00 00 00 00 00 00 00 00 00 00 C4 08 00 00 ...
   |-----|-----|-----|-----|-----------------|-----------| 
      |     |    |      |    file attributes   LFH offset
 filename   |  comment  |    
 length     |  length   |
          extra       file 
          field       start
          length      disk 

```
```
... 78 6C 2F 77 6F 72 6B 73 68 65 65 74 73 2F 73 68 65 65 74 31 2E 78 6D 6C ...
   |-----------------------------------------------------------------------|
     filename ('xl/worksheets/sheet1.xml')
     24 bytes (0x18) in this case
```
```

At the end of the entry comes ZIP64 section foretold by `0x0C00` in extra field length field. 

... 01 00 08 00 9A 90 DC 15 01 00 00 00 
   |-----|-----|-----------------------|
    ZIP64    |      uncompressed size
    field    |
   signature |
             |
         field
          size    
```

```
void writeCEN(ZipEntry entry) throws IOException {
  boolean useZip64 = entry.size > 0xffffffffL;
  writeInt(0x02014b50L);                         // "PK\001\002"
  writeShort(45);                                // version made by: 4.5
  writeShort(45);                                // version required: 4.5
  writeShort(8);                                 // flags: 8 = data descriptor used
  writeShort(8);                                 // compression method: 8 = deflate
  writeInt(0);                                   // file modification time & date 
  writeInt(entry.crc);                           // CRC-32
  writeInt(entry.compressedSize);                // compressed size
  writeInt(useZip64 ? 0xffffffffL : entry.size); // uncompressed size 
  writeShort(entry.filename.length());           // filename length
  writeShort(useZip64 
    ? (2 + 2 + 8)  /* short + short + long*/
    : 0);                                        // extra field len
  writeShort(0);                                 // comment length
  writeShort(0);                                 // disk number where file starts 
  writeShort(0);                                 // internal file attributes (unused)
  writeInt(0);                                   // external file attributes (unused)
  writeInt(entry.offset);                        // LFH offset
  out.write(entry.filename.getBytes(US_ASCII));  // filename characters
  // Extra field:
  writeShort(0x0001);                            // ZIP64 field signature
  writeShort(8);                                 // size of extra field (below)
  writeLong(entry.size);                         // uncompressed size
}
```

Excel actually seems to **ignore** flag value here. LFH takes precedence.
Files open fine irrespectively if flag value is 0 or 8 here. 
But in LFH, the flag value **must** be 8 is ZIP64 is used.

### End of central directory record

Nothing out of the ordinary here as well. Standard stuff:

```
 50 4B 05 06 00 00 00 00 08 00 08 00 09 02 00 00 18 6F 30 21 00 00
|-----------|-----|-----|-----|-----|-----------|-----------|-----|
  END sig      |      |     |    |            |   CEN offset   |
               |      |     |   total number  |                |
          disk number |     |   of entries    |      archive comment
                      |     |                 |      length
                disk number |        size of CEN
             containing CEN |        in bytes
                            |
             number of entries
             on this disk

```
CEN offset is the byte number at which the central directory structure starts. 
<!--
As you can see, zip allows for fast access to list of files in the archive as well as
specific file data. Without scanning through the whole archive. As long as 
seeking (jumping around) is possible. 
-->


```
void writeEND(int entriesCount, int offset, int length) throws IOException {
  writeInt(0x06054b50L);    // "PK\005\006"
  writeShort(0);            // number of this disk
  writeShort(0);            // central directory start disk
  writeShort(entriesCount); // number of directory entries on disk
  writeShort(entriesCount); // total number of directory entries
  writeInt(length);         // length of central directory
  writeInt(offset);         // offset of central directory
  writeShort(0);            // comment length
}
```

## Conclusions 

It seems critical to Excel that the zip specification version is 4.5 in Local File Header if ZIP64 is used anywhere
with this zip entry (Central directory file header or Data descriptor). 
