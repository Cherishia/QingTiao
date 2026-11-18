package dev.local.qingtiao;

/** Control-only recognition for the supplied notice footer and delayed upper-left circled X. */
public final class PicaVisual {
    // Only text-control glyphs and the X stroke signal are embedded. No ad pictures or links.
    private static final float[] NEVER=decode(new String[]{
        "fbf8f6f5f4f4f3f3f4f6f8f8f7f8f8f8f8f3f8fcfaf5e6ebeaeaeeebe9ecebeae8e9eae9eaecf1f8f9f6eee9eae9e9e9ebeaeef1e3e7ebeaebebedf5f9f8fcf6edeaeaeaeaeaeaeaeaeaeaeaeaebf6fc",
        "f5e5d7d9d7d6d6d7d7d5d5d6d5d4d3d4d4d3e7f7f4e5c8c5c6c5c7cbc5c3c6c7c6c6c6c7c8c9dcf3f8f6dec8cacdcacccbc5d1d9c6cac8c1c7c7cde5fbfefae7d0c9c9c9c9c9c9c9c9c9c9c9c9c9ecfc",
        "efd3b9bbb8b6b9bcbcb8b2b5bbbcbbb9b8c0def4f5e8cfcccececbd1c6b6b9c7d0cecccecfcde1f6f8f4d2bcced5d4d3cfc2ccdbd2d0bdb6ccd2d5e8fafcfae9c9bebebebebebebebebebebebebee9fc",
        "f4e2d7d8d4d3d8d8cdbbb6c1d0d5d5d4d5d9eaf6f8f5edeceaece8eadfc3c0d9eaeae8ebefeef5f9f5f2d0bacedadbd9d2bfcde8e6dec2bcd9e7eaf4fafafdf6e9e4e4e4e4e4e4e4e4e4e4e4e4e5f4fc",
        "fbf6f4f5f3f3f4ebd5bec6dcedf0f1f3f6f4f8f9f7fbf5e4d9e0d9dcd5bcb9cedddfdddee5f3fbf8f5f2d3bfd0dcdcdad4bfcbe3d9cdbbb7c8ccd8f3fdfdfefaf9fbfbfbfbfbfbfbfbfbfbfbfbfbfbfc",
        "fcfcfafbf9f9f8e6cabcd1eefaf9f9fbf9f9fcfcf9fbeccebac2bcc2c2b4b7bdc3c3bfbbc5e6fbfbfbf2cfc0d2dedcd9d5c1cdddc7c2cacfcdbdc9eefcf7f9faf9f9f9f9f9fafafafafaf9f9f9f9fafc",
        "fcfdfbf7f9f8ebccb4bdddf7f6fcfcf9fdfdfcfcfafaedc6bbd9e1e1d8c2bfd5dee0d9bfc4e7fafefbf7dabcb9cacbc6c2bfd7e4c7cbe9ece7c7beeffcf2ddd0d2d3d2cfcfd6d2d0d3d3d6d5ced3d0dc",
        "fbfbf7fafaead2bcb0b9daf2e3eaf9fcfcfdfcfcfcf9edcdbeddecefe7cac2e1eeefe5c4bfe5fcfcfbfae6c8c8d8e3d3c3d2e5e9c6bcd1cfcabec1f1fbe9cebeb8b8bbb8b3b8b4b0b7b4b4b9b9bdb8c8",
        "fdfcfaf8edcfbcbfbcb7d1e3c8c4dff1f6fbfcfcfefcecc6b3c7cbcdc8b8b7c7cbcccbb7bbe5fcf7fef6d8c5d4d7d6cccadadee3c4bed3d1cdbcbef2fdf4e3dbdadadee0d9d2c1b8ccd8d8dddedcd7e2",
        "fdfbf7e6ceb9c2d5d1bccde2cbbbc2d5e9f6fcfbfcfdeec9bacac5c7c8bbb9c5c5c6cab9bee5faf2fcedc6b8c4c4bbb9c2c7d2dec6c9e4e6e3c4bef3fefdf8f6f6ede9f5f5e6c5b8d8f1efeceff3f6f9",
        "f8f0dfc6b9bedbe8d7bbcfeee9d5bdbacce4f5fafbfdeec9bfdbe5eae3c4bddbe9eadfbebee7fbf9fcefd8c8bdcfd8d1c4c2e0eac8bdcecac6bbc1f3fcfdfbf8f1dbd2eaf9edc8b9dcf2dfd1dff3f8f9",
        "efd8bfbbc4daf1f0d3b6cef2f8eed3bdb4c7e6fbfcf3e5c7bfdbe9efe2c4c1ddeae9e0c3bfdbf0fcfdf5e9d1c2d6e7dbc4c7dfe5c5c1dbdbd7c1c0f2fbfefcf4dcc1c3e4f8eccbbbddf2d4bac4e0f3f9",
        "e3c3b6cae2f2f9f3d4bdd0eff6f8f0dec4c1e0fbf4d2c2b5b2c4c0c5c5b6b6c1c1c1c4b5b3bcc9ecfcf0d4c1d0cdc6c3c9cfd0dac5d0f0f0e8c7bff2fcfefaebcabad4f8fbeac9b8dff9edc7b9c9ecfa",
        "f3e6d8eaf5f8fcf7d7b8cff3fbfbf8f6eeedf7fdf2d5c4b8b0c0c8c0c1c1c2c3bec4bfb3b5bcc7eef8eccab9c0b9b7bdbfbfc0cfc6b3bdc0beb9c0f1fdfcf2d2b6cdecf9feedcbbadffbfde8c0b3d0ee",
        "f9f7f7fbfdfbfdf8d6b6cdf3fcfdfcfefefdfefffbf2e5c9c2e2f3f3f6f7f8f6f2f3e4c9c4e2f7fdfcf7e8e6ebe3dddee7e3d9ded5c4cbc8c2c5d8f5faf7ddc1c3dff8fefdebc3bbe1f8f9fadfbcb2d1",
        "fdfcfdfcfdfbfef9d8b8cef4fbfbfcfdfcfcfdfefbf7eec9bfeafafbfbf9f9f7f3f3e3bebae5fbfbf7f2d8dbdfdce1dadfd5d3e8e4d3e3ecd8d5e9fbfaf4cfbbd8f1f6efeddfc2b9e0f8f7fef4d7bdd3",
        "fefefdfdfcfbfef8d9b9d0f5fcfbfcfcfcfcfcfdfbf6eec9beecfafafaf8f9e9d3d0cabcc7edfafafbeac4d3d6ccddd2d5d8cacfcabfd8e8cac2cfeefcf7e5dceef9e7c8c2beb5bde2f8fbfdfbf3e6ef",
        "fffffefefffefef7dbbfd4f6fbfdfefcfdfcfbfcfefaeecbc3eaf7fafcfaf7e0bfb5bbc2d9f4f9fdf7e1cae1e2d3e6dfdfece0cac3daf2f8ebd1c0e6fafbfaf6f7fae5c2babcc0d9effafefefefefafa",
        "fffef9fafefdfefbecd8e4fafafefdfafbfbfafcfdfdf8e5e1f4fcfbfdfcfcf5e9e5e5e7f0f9fdfdf6ece7f1f1eef8f6f3f7f6e9e3f5fefaf7eee5f5f9fbfbf6f5f9f2e4e5e8ecf7fbfdfefefffefaf8",
        "fefefdfdfefdfefcf8f3f6fdfafdfbf8fafcfefdfdfdfdfbf9fdfcfafcfbfbfcf7fafaf9f9f9f9fcf9fcfaf7f9fbf9fafbfbfaf7f8faf7fcfbf8fafcf4fcfaf7f7f9f9f6fafcfcfbfbfefdf9fdfdfcfa"
    });
    private static final float[] CONFIRM=decode(new String[]{
        "fafdfbfafcfafcfbfafdf9f8f9e9f1f9f7fbfefbfdfdfdfefcf8fcfefef7ebe9f4fbf9faf8f7fcfc",
        "fafcf9f5f4f4f5f5f5f7f7faedd0d7f1f9fcf8fcfdfbfafbfbfbfcf7f9eed2c6d8f6f6fafdfcfbf9",
        "fcf4e3d6d5d5d7dde7e8eae8d3bfcae3eaeae5f3fafbf6ece6e7ebe5e8e1c4b6c8e6e6e8eae8e8ef",
        "fdf1d7c2bbb9c3cdd4c8c8c4bab9c0c6c7c6c9e3f5faefd1c1c7c5c4c7c4b8b2b9c6c6c5c6c1c6e0",
        "f9f7eed8bdc1dce5dabcc5cab4c2c9cbcfc5b5dbf2f8e5bfb4c6c7cbcac8cacbc6c7ccc9c9b9b2d3",
        "f5fafae8c2cbedf1e0c3d5d7b8d3dbdeebe0c0e2f4f7e5c6c4e0eff3f1eff1f1efeef1eeecd4bcd8",
        "fcfdf7ddbad7f2f1eadbe4c8bfddd4c4dbe9e1ecf6f9eed7d8edf6f6f7f8f4f3f5f6f5f2f4e5cee7",
        "f6fcf6ccb5d9e9eefaf2d5bcc0d1ccc1c8d4dbe6f8faeed1cecccdd2d1cecececccdcececccad0dc",
        "fcf7e6beb4c5c3d5f1e0c7bec0cabfb9c5cac7ebf8f8ecc9c1c4bfc0bebebbb5b8bfbdbcbcbec8d7",
        "fbedceb6bccfbcc9e2cbbfc8e1e1c9c0d5e4e8f5f9faf8e3dedde0e4e3e0d0bdc5e2e3e3e3e3e6ee",
        "f8e6beb5c6e4c7c0ceb6b7c4d8d9c3bbd0dee1f3fcfbfcf7f4ded9eef6f2d8bdccf4f8f8f8f5f6f9",
        "f4e7c1bacaeacbbcccbab7c0c2c5b4afbfc6c2ecfdfaf9f9f3c8c1ebfaf5d8b9cae7e8e7e7e9f2fc",
        "f6f1d7c3c8eacfc4e3d5c0cbdfe1cbc2d5e3e5f4fcfbfcfdf3c2bdeffaf4dab4bdc1c1c0bec2d5ec",
        "fcfcf0cdcaebcfc8f2e1bdcde2ddc6c1d7e6e9f7fcfbfdf8e7bebff0f8f3ddb9bcd0d4d5d0d0e0f5",
        "fbfaf0c7cdebcfccf1e5bfb8bcbcb8b7bbc0c1eafbfaf9f2d2b9badaf3f2d6bccbf2f7f7f6f7fcfd",
        "fcfbf3cac3d8c2cdf4e4c3c7dbdbc7bccee1e2f5f9f8f9e7c2b6b7c2dcedd8bdcaf3f9f9f9f8fafb",
        "fbfcf3c7b6bfb1cef7e1c1cbe1e0cac1d5e6e5eff7fbf3cfb8cecebcbed0c8b9c9e4e9eae9e6e8ea",
        "fdfbf0cabccfbecdf4e5bfb8c5bfb6b7c0c5c3dcf3fae1bac1e8eedac0bab6b1bbc2c4c4c3c2c5c7",
        "fefcf3ddd9ede2e3f4e4c8c7dcdcdbdedcd9deeaf8f9dec9ddf3f9f6e4d6d2cac6c1c1c0bfc0c5c9",
        "fefffcf3f5f9f8fbf9e9e2effaf8f8f8f5f5f9fcfdfcf0ebf5f7f8fdfbf8f6efedf2f4f2f0f0f4f6"
    });
    private static final float[] BOARD=decode(new String[]{
        "c3c6c5c6d2cdc8d4e2e8e0e8e4e3dbc9c4c6c6c4c4c6ccdcdadae4e5b899c9dac5c8c3c6c2c6c7c6c9cfd27d7ed3d1d2dfcfc9c2aea589745a62a5ca",
        "c2c7c4dfd7d9dee9ab8c847f7c98d3d1c3c4c6c7c5c3d5c87f86e1e58854aee0cfd7d3d5d5cec8c1c8d3cb696cc8cddcb8685d59595759636b84aecb",
        "c5c7c9e4907ccde5895f615f5355afd9c8c3c4c6c4c8df995387d6d37f4f98d1cfcfcdcecfdfcfc9d6dfce6f6ccadaebb25370a0adb2afb7b1ac9686",
        "c8c3d8cf5c56c0dcd6beb8bb984d73cdd3c9c2c6ccd9b356435c68645347555e5e605e5773cdcfd6d5baa0615891a5c4b15598d6b1a6969186847e70",
        "c7c8df9d4d80c29eb4947b9fba754a98dbcfc4cadbc36a4f6a7678795c446382878a8d919fcfc1d8af5e4f46484f5791a95787c6c2bfc4c0bdbecebe",
        "c5d5cb6750a9b1a1bfb7aab8d2a84f5caddbcdcedb854a88c3cac4cf914b99cfb9b7b4b7b1a5a9cdcfb49c5b5593bcd2b6574f5f5d5d605d595fa4c8",
        "ccd88b4876c4c3cbd6dad0c1d3d390515fafdcdbe89b81c7d3b7bfc9914c90c6b9b6b3b4b6bbc9c4bbceb65d4880c8e4ba58534b4d838e8f6d559cb9",
        "e0a15454acbbbe8e86c6dbcbc8d5d0794d63beefc08a808c8f92888461485c767771716d676fa5cdaabf8d45444d7fcbbe5b75685abac8c7885ea6a0",
        "b95f5397b3a3b05355b4ccc1c6cbd9c67b6dbbe4a4606164666768675a455971767a7b7e8283aac5bcb5613f54796485a6558c93509ac7c0736ab48b",
        "cd8c90b686b88e4c78b6a6c0d7d8d5dcc9b6afb7d0bdc3c7cfcdd3cd97578bd4e1e3dfe1e0c2a9abc185524f5bad9593a45893bf5b70d6bf5e7bac7e",
        "d8ccb8828fc76350a2a6a1d1baafd5d3ceb97ea1c8cedfd6d5d4d1cbaa869abec4c1bcb9c6ba7ba4ab5b74725ac2dcdca853a0e2894e998e549d9b79",
        "c8c29c80c2ac536ab890b5ce835aa8dbc8b4a0bcc1d2ba736d6a6766635c5e63625d5d5686c997b7b36aa7865ab8c7d59e56a9d2c06357556ab2827b",
        "c2c7bcc4d1704f95ab8bc3d4a5546bc3d4c4c0b5b0cead4c65898f8f929496969a9d8d506fc999a6cdbadb8f5eb2b5c8875abba5cd9748498aba878a",
        "c8c3c5deb04a6aba98a4c7d5d27a4898d9b9a79898c3b34f8bccb5aeabaaa7a5a1bec05d6fc69596bccad3915bafccc56f68c9c3c486444566acc3b0",
        "d1d5e3d4744c9bd9c4d4d4d0c78b4961c1bb999596bfb54b89cebfbbbabdb9b5b5beb3576dc69591939bb89358afdfaa5887d4b5784f6f7e575c91c8",
        "e1c4ad874a4a79897e7470675a5446488fca9d9393bbb54d5b7379736f6e696768625c436ec7948f9399bd965bb2df87569c93594f7fbfbea05f5fb3",
        "e2915248474d55585b666e7a8896884e60b9b49394bcbb4e506b6c7073777b7e80857d506ac596909499bf9c5aade3a38bcf8a669fb17d7abbb6aeb9",
        "e3c09b99a5adb5bcbbb6b2b0a4a6bc7968bcc39294b6c26e8dc6b6b1b0afadacadb9d09d9eca92939197bfc2a4c6c4c6d1bbc8c49a5e577795b0ae7a",
        "d3d5bc9893827b736c64605e5d6db8c8b5ae948c90a9d0c4b7916363636363626470adbfb0907c8f9295a9bfac8c73949271988159618193918d8372",
        "baae825e60656a7176797e878e8b9ba27152658d8f939c89675975878889898a8a89928167607e93969894907967738f8e8788827f8b8f90969a9ea9"
    });
    // Local contrast of the close X only; matching excludes the surrounding image.
    private static final float[] X_DETAIL={
        5.78f,29.33f,25.33f,1.11f,14.11f,29.67f,16.67f,27.78f,30.33f,31.33f,37.11f,33.56f,24.44f,27.11f,39.78f,28.44f,24.22f,18.00f,17.56f,14.56f,11.78f,19.33f,28.00f,35.22f,
        -9.44f,-8.56f,-22.00f,-46.11f,-33.22f,-27.56f,-52.11f,-13.56f,-8.56f,-13.33f,-13.33f,-9.67f,-7.22f,-7.22f,-8.44f,-12.22f,-31.78f,-42.22f,-38.11f,-36.22f,-40.67f,-29.00f,-8.67f,-10.33f,
        3.33f,-43.44f,-33.00f,53.78f,36.11f,-56.11f,-74.44f,-47.22f,-24.44f,-18.11f,-25.22f,-23.67f,-16.22f,-20.00f,-36.00f,-31.56f,-72.11f,-58.33f,8.33f,36.56f,-2.11f,-35.22f,-32.11f,-26.78f,
        6.22f,-37.33f,-29.33f,93.22f,142.78f,50.33f,-50.00f,-69.33f,-39.89f,-12.33f,-11.11f,-11.22f,-7.78f,-12.56f,-26.00f,-52.89f,-60.22f,9.67f,111.89f,113.22f,25.11f,-28.78f,-17.44f,-6.44f,
        -9.22f,-18.33f,-36.11f,5.89f,112.56f,125.44f,34.00f,-38.00f,-42.44f,-37.00f,-16.22f,-3.89f,-9.67f,-25.44f,-26.56f,-48.56f,8.00f,83.78f,115.78f,60.22f,-17.11f,-34.11f,-8.33f,7.00f,
        -8.33f,-10.67f,-32.78f,-53.78f,-4.11f,87.67f,132.00f,28.11f,-27.78f,-54.00f,-32.00f,-9.67f,-20.11f,-41.67f,-46.56f,-7.67f,77.78f,115.78f,47.00f,-38.67f,-54.89f,-27.44f,-10.22f,3.44f,
        3.56f,-7.00f,-14.00f,-30.22f,-60.22f,-17.22f,86.89f,134.00f,41.44f,-52.67f,-49.00f,-30.89f,-59.33f,-49.22f,-8.33f,87.11f,108.33f,40.33f,-41.67f,-43.44f,-31.11f,-25.11f,0.22f,-1.44f,
        9.22f,-5.67f,-11.56f,-14.78f,-46.22f,-47.44f,-17.56f,88.78f,126.78f,51.00f,-64.33f,-93.56f,-79.22f,-4.89f,85.78f,116.67f,40.67f,-33.78f,-51.33f,-25.78f,-12.33f,-7.22f,6.56f,-2.33f,
        7.11f,-5.44f,-9.11f,5.89f,-12.78f,-46.44f,-58.00f,-17.89f,84.89f,117.22f,19.56f,-71.33f,-28.67f,75.33f,114.22f,44.89f,-33.78f,-60.56f,-32.11f,-8.33f,-6.00f,0.89f,3.44f,0.00f,
        4.33f,-0.22f,-3.89f,7.22f,3.78f,-16.89f,-30.56f,-62.56f,-18.44f,71.00f,94.22f,36.89f,50.67f,88.89f,40.89f,-62.22f,-56.44f,-32.00f,-5.44f,-0.22f,-0.33f,5.22f,-0.67f,0.22f,
        2.00f,-0.44f,0.00f,1.89f,-2.56f,-8.56f,-14.67f,-54.33f,-80.56f,-42.56f,48.44f,86.00f,64.33f,3.00f,-74.78f,-87.11f,-35.33f,-9.11f,-10.11f,3.67f,11.33f,3.33f,-4.00f,0.56f,
        -2.11f,-4.89f,3.89f,5.11f,-3.11f,-7.56f,-25.00f,-60.22f,-100.22f,-48.89f,46.33f,80.78f,71.22f,20.22f,-87.11f,-81.78f,-39.00f,-12.78f,-8.78f,7.56f,1.22f,-7.78f,3.67f,-2.22f,
        -4.78f,-5.44f,4.22f,2.78f,-2.56f,-19.33f,-41.67f,-43.89f,-18.78f,63.11f,89.33f,32.33f,62.78f,105.89f,30.56f,-41.44f,-48.44f,-30.33f,-0.67f,2.33f,-8.00f,-2.78f,3.33f,1.67f,
        -4.56f,3.11f,1.22f,-13.33f,-19.00f,-26.56f,-51.00f,-23.56f,86.78f,133.11f,34.44f,-74.56f,-33.56f,78.11f,119.78f,33.67f,-33.22f,-44.00f,-14.44f,-18.67f,-3.22f,14.56f,-8.89f,-1.67f,
        -1.78f,10.56f,-7.89f,-18.11f,-30.11f,-55.22f,-21.11f,89.56f,129.00f,42.22f,-67.33f,-87.67f,-82.78f,-28.56f,89.78f,115.22f,35.44f,-41.89f,-48.78f,-20.11f,-12.44f,-9.89f,3.67f,1.78f,
        8.00f,-10.00f,-17.89f,-27.56f,-62.44f,-14.78f,94.00f,115.22f,46.78f,-36.44f,-67.89f,-49.11f,-52.11f,-45.78f,-6.22f,84.00f,97.22f,53.89f,-31.00f,-56.11f,-21.22f,0.33f,-7.44f,-3.67f,
        5.89f,-20.11f,-35.44f,-56.67f,-10.67f,99.89f,125.11f,54.33f,-33.78f,-63.22f,-36.78f,-13.89f,-22.56f,-40.00f,-50.33f,0.56f,92.56f,117.22f,37.78f,-35.89f,-43.11f,-20.67f,-11.22f,1.00f,
        2.67f,-17.33f,-32.22f,7.00f,111.89f,145.00f,44.89f,-40.89f,-59.22f,-37.11f,-1.78f,0.00f,-4.33f,-11.89f,-32.33f,-55.44f,4.44f,79.33f,115.44f,67.89f,-16.67f,-37.67f,-2.44f,3.67f,
        8.89f,-21.56f,-19.11f,107.56f,152.89f,39.56f,-33.11f,-62.44f,-33.89f,-11.33f,1.67f,8.56f,7.56f,-6.78f,-22.67f,-29.89f,-54.78f,-1.11f,111.44f,133.22f,36.56f,-23.56f,-1.44f,3.00f,
        0.78f,-19.78f,-26.78f,56.67f,71.44f,-32.33f,-60.56f,-30.22f,-5.67f,-3.56f,-4.89f,8.00f,-1.44f,-13.89f,-3.33f,-12.44f,-44.00f,-33.78f,29.67f,58.22f,11.67f,-22.11f,-10.89f,-2.11f,
        -8.89f,-16.44f,-25.89f,-30.33f,-15.22f,-20.33f,-45.67f,-11.56f,3.44f,5.00f,1.22f,-0.11f,-5.56f,-1.78f,10.00f,-14.33f,-16.33f,-24.11f,-25.44f,-23.78f,-32.78f,-30.56f,-11.44f,-6.44f,
        -7.33f,-21.78f,0.11f,-1.56f,-33.89f,-21.22f,-8.22f,-8.33f,-1.44f,8.89f,2.11f,-8.00f,7.89f,11.11f,-12.89f,-0.33f,-1.33f,-16.67f,-21.44f,-15.22f,-18.11f,-16.11f,-7.56f,-10.00f,
        -27.00f,-5.56f,6.22f,3.00f,-5.22f,-4.00f,5.44f,0.89f,1.89f,0.56f,1.44f,2.00f,0.33f,1.44f,0.22f,1.67f,-1.44f,-1.00f,-0.11f,3.44f,4.44f,-3.22f,-19.67f,-23.11f,
        -32.67f,-27.44f,-24.22f,-5.11f,14.67f,13.22f,0.56f,1.44f,1.11f,1.00f,-1.00f,-1.78f,0.67f,-0.22f,0.89f,-1.11f,8.11f,7.67f,2.78f,-0.67f,-0.22f,-11.00f,-30.44f,-56.44f
    };
    private static float[] decode(String[] rows) {
        int width=rows[0].length()/2;float[] values=new float[width*rows.length];
        for(int y=0;y<rows.length;y++)for(int x=0;x<width;x++)values[y*width+x]=Integer.parseInt(rows[y].substring(x*2,x*2+2),16);
        return values;
    }
    public static final class Result {
        public final BiliVisual.Result target;public final int kind;public final boolean waiting;
        Result(BiliVisual.Result target,int kind,boolean waiting){this.target=target;this.kind=kind;this.waiting=waiting;}
    }
    private static int light(int p){return (((p>>16)&255)*77+((p>>8)&255)*150+(p&255)*29)>>8;}
    private static boolean pink(int p,boolean pale) {
        int r=(p>>16)&255,g=(p>>8)&255,b=p&255;
        return pale?r>=155&&r-g>=17&&b-g>=5&&b>=130&&r-b<=65:r>=90&&r-g>=25&&b-g>=12&&r>b;
    }
    private static boolean white(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return Math.min(r,Math.min(g,b))>225&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<25;}
    private static boolean grayBright(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return light(p)>135&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<45;}
    private static double correlation(int[] p,int w,int x,int y,int tw,int th,float[] template,int sw,int sh) {
        double worst=1;
        for(int part=0;part<2;part++) {
            double a=0,b=0,aa=0,bb=0,ab=0;int n=sw*sh/2;
            for(int sy=0;sy<sh;sy++)for(int sx=part*sw/2;sx<(part+1)*sw/2;sx++) {
                double v=light(p[(y+Math.min(th-1,(int)((sy+.5)*th/sh)))*w+x+Math.min(tw-1,(int)((sx+.5)*tw/sw))]);
                double t=template[sy*sw+sx];a+=v;b+=t;aa+=v*v;bb+=t*t;ab+=v*t;
            }
            double va=aa-a*a/n,vb=bb-b*b/n;
            if(va/n<8||vb<=0)return -1;
            worst=Math.min(worst,(ab-a*b/n)/Math.sqrt(va*vb));
        }
        return worst;
    }
    private static int[] labelBox(int[] p,int w,int l,int r,int t,int b) {
        int x0=r,y0=b,x1=l,y1=t;
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)if(pink(p[y*w+x],true)) {x0=Math.min(x0,x);x1=Math.max(x1,x+1);y0=Math.min(y0,y);y1=Math.max(y1,y+1);}
        return new int[]{x0,y0,x1,y1};
    }
    private static boolean header(int[] p,int w,int h) {
        double best=-1;int bx=0,by=0,bh=0;
        for(int th=Math.max(14,(int)(w*.044));th<=w*.062;th+=2) {
            int tw=Math.round(th*87/31f);
            for(int y=(int)(h*.075);y+th<h*.125;y+=2)for(int x=(int)(w*.64);x+tw<w*.85;x+=2) {
                double score=correlation(p,w,x,y,tw,th,BOARD,60,20);
                if(score>best){best=score;bx=x;by=y;bh=th;}
            }
        }
        if(best>.45)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dh=-1;dh<=1;dh++)
            best=Math.max(best,correlation(p,w,bx+dx,by+dy,Math.round((bh+dh)*87/31f),bh+dh,BOARD,60,20));
        return best>=.70;
    }
    private static double word(int[] p,int w,int h,int[] box,float[] letters,int sw) {
        double best=-1;
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++) {
            int x=box[0]+dx,y=box[1]+dy,tw=box[2]-box[0]+dw,th=box[3]-box[1]+dh;
            if(x>=0&&y>=0&&tw>0&&th>0&&x+tw<=w&&y+th<=h)best=Math.max(best,correlation(p,w,x,y,tw,th,letters,sw,20));
        }
        return best;
    }
    private static BiliVisual.Result notice(int[] p,int w,int h) {
        int start=-1,last=-1;
        for(int y=(int)(h*.84);y<(int)(h*.98);y++) {
            int a=0,b=0;
            for(int x=(int)(w*.14);x<w*.45;x++)if(pink(p[y*w+x],true))a++;
            for(int x=(int)(w*.60);x<w*.86;x++)if(pink(p[y*w+x],true))b++;
            boolean row=a>=3&&b>=2&&a<w*.25&&b<w*.20;
            if(row){if(start<0)start=y;last=y;}
            if(start>=0&&(!row&&y-last>2||y>h*.98-2)) {
                int height=last-start+1;
                if(height>=h*.009&&height<=h*.030) {
                    int[] left=labelBox(p,w,(int)(w*.14),(int)(w*.45),Math.max(0,start-2),Math.min(h,last+3)),right=labelBox(p,w,(int)(w*.60),(int)(w*.86),Math.max(0,start-2),Math.min(h,last+3));
                    int lw=left[2]-left[0],lh=left[3]-left[1],rw=right[2]-right[0],rh=right[3]-right[1];
                    if(lw>w*.10&&lw<w*.25&&rw>w*.05&&rw<w*.13&&lh>0&&rh>0
                        &&word(p,w,h,left,NEVER,80)>=.72
                        &&word(p,w,h,right,CONFIRM,40)>=.72) {
                        int pale=0;
                        for(int i=1;i<=9;i++)for(int dy:new int[]{-height/2,height+height/2}) {
                            int sy=start+dy;if(sy>=0&&sy<h&&white(p[sy*w+(int)(w*(.08+i*.084))]))pale++;
                        }
                        boolean line=false;
                        for(int sy=Math.max(0,start-height*2);sy<start-height/3;sy++) {
                            int count=0;for(int x=(int)(w*.08);x<w*.92;x++)if(pink(p[sy*w+x],true))count++;
                            if(count>w*.5){line=true;break;}
                        }
                        if(pale>=14&&line&&header(p,w,h))return new BiliVisual.Result((left[0]+left[2])/2,(left[1]+left[3])/2,lh,"公告板字形、白色弹窗页脚及‘不再顯示/確定’成对匹配；点击不再顯示");
                    }
                }
                start=-1;last=-1;
            }
        }
        return null;
    }
    private static boolean adFrame(int[] p,int w,int h) {
        int status=0,total=0,dark=0;
        for(int y=(int)(h*.012);y<h*.045;y+=Math.max(1,h/300))for(int x=(int)(w*.12);x<w*.88;x+=Math.max(1,w/80)) {
            int v=p[y*w+x];total++;if(pink(v,false)&&((v>>16)&255)>170)status++;
        }
        if(total==0||status<total*.65)return false;
        for(int y=(int)(h*.06);y<h*.10;y+=3)for(int x=(int)(w*.15);x<w*.90;x+=4)if(light(p[y*w+x])<90)dark++;
        if(dark<w*h*.0005)return false;
        int first=-1;
        for(int y=(int)(h*.055);y<h*.115;y++) {
            int count=0;
            for(int x=(int)(w*.01);x<w*.99;x++)if(pink(p[y*w+x],false))count++;
            if(count>w*.55) {
                if(first<0)first=y;
                else if(y-first>h*.022&&y-first<h*.065)return true;
            }
        }
        return false;
    }
    private static BiliVisual.Result cross(int[] p,int w,int h) {
        int rw=(int)(w*.14),top=(int)(h*.052),bottom=(int)(h*.16);boolean[] seen=new boolean[rw*(bottom-top)];int[] q=new int[seen.length];
        BiliVisual.Result found=null;
        for(int y=top;y<bottom;y++)for(int x=0;x<rw;x++) {
            int at=(y-top)*rw+x;if(seen[at]||!grayBright(p[y*w+x]))continue;
            int begin=0,end=1,l=x,r=x,t=y,b=y;q[0]=at;seen[at]=true;
            while(begin<end) {
                int v=q[begin++],sx=v%rw,sy=top+v/rw;l=Math.min(l,sx);r=Math.max(r,sx);t=Math.min(t,sy);b=Math.max(b,sy);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=sx+dx,ny=sy+dy;if(nx<0||nx>=rw||ny<top||ny>=bottom)continue;
                    int n=(ny-top)*rw+nx;if(!seen[n]&&grayBright(p[ny*w+nx])){seen[n]=true;q[end++]=n;}
                }
            }
            int cw=r-l+1,ch=b-t+1,cx=(l+r)/2,cy=(t+b)/2;
            if(cw<w*.018||cw>w*.055||ch<cw*.7||ch>cw*1.4||end<cw*ch*.08||end>cw*ch*.55)continue;
            int near=0;int[] corners=new int[4];
            for(int i=0;i<end;i++) {
                double dx=(q[i]%rw-l+.5)/cw*2-1,dy=(top+q[i]/rw-t+.5)/ch*2-1;
                if(Math.min(Math.abs(dx-dy),Math.abs(dx+dy))<.35)near++;
                if(Math.abs(dx)>.45&&Math.abs(dy)>.45)corners[(dx>0?1:0)+(dy>0?2:0)]++;
            }
            if(near<end*.80)continue;boolean complete=true;for(int n:corners)if(n<1)complete=false;if(!complete)continue;
            boolean circle=false;
            for(double radius=cw*.85;radius<=cw*1.4;radius+=Math.max(.5,cw*.04)) {
                int ring=0,inside=0;
                for(int i=0;i<24;i++) {
                    double a=i*Math.PI/12;int sx=(int)Math.round(cx+Math.cos(a)*radius),sy=(int)Math.round(cy+Math.sin(a)*radius);
                    int ix=(int)Math.round(cx+Math.cos(a)*radius*.74),iy=(int)Math.round(cy+Math.sin(a)*radius*.74);
                    if(sx>=0&&sx<w&&sy>=0&&sy<h&&light(p[sy*w+sx])>95)ring++;
                    if(ix>=0&&ix<w&&iy>=0&&iy<h&&light(p[iy*w+ix])<95)inside++;
                }
                if(ring>=18&&inside>=18){circle=true;break;}
            }
            if(!circle)continue;
            if(found!=null)return new BiliVisual.Result(0,0,-1,"多个圆圈X，未点击");
            found=new BiliVisual.Result(cx,cy,Math.max(cw,ch),"粉色顶栏、黑底粉框与左上角圆圈X匹配；X已出现，无需额外等待");
        }
        return found;
    }
    public static boolean trusted(String pkg){return "com.picacomic.fregata".equals(pkg);}
    /** One-way layout signal from static notice text rows; never retains pixels or text. */
    public static long noticeSignature(int[] p,int w,int h) {
        long signature=0;double[][] cells=new double[8][9];
        for(int y=0;y<8;y++)for(int x=0;x<9;x++) {
            double sum=0;int count=0;
            int l=(int)(w*(.10+x*.80/9)),r=(int)(w*(.10+(x+1)*.80/9));
            int t=(int)(h*(.24+y*.20/8)),b=(int)(h*(.24+(y+1)*.20/8));
            for(int py=t;py<b;py++)for(int px=l;px<r;px++){sum+=light(p[py*w+px]);count++;}
            cells[y][x]=count==0?0:sum/count;
        }
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)if(cells[y][x]>cells[y][x+1]+.75)signature|=1L<<(y*8+x);
        return signature;
    }
    public static Result detect(int[] p,int w,int h){return detect(p,w,h,"");}
    public static Result detect(int[] p,int w,int h,String pkg) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("画面尺寸不适用",false);
        BiliVisual.Result n=notice(p,w,h);if(n!=null)return new Result(n,SplashActions.PICA_NOTICE,false);
        boolean known=trusted(pkg);
        if(!(known?pinkHeader(p,w,h):adFrame(p,w,h)))return none("未匹配哔咔公告或开屏关闭上下文",false);
        BiliVisual.Result x=known?contrastClose(p,w,h):cross(p,w,h);
        if(x!=null&&x.size<0)return none("多个关闭图形，未点击",true);
        return x==null?none("哔咔开屏上下文已确认，左上角尚未定位圆圈X，继续等待",true):new Result(x,SplashActions.PICA_CLOSE,false);
    }
    private static boolean pinkHeader(int[] p,int w,int h) {
        int hit=0,total=0;
        for(int y=(int)(h*.012);y<h*.045;y+=Math.max(1,h/300))for(int x=(int)(w*.12);x<w*.88;x+=Math.max(1,w/80)) {
            int v=p[y*w+x];total++;if(pink(v,false)&&((v>>16)&255)>170)hit++;
        }
        if(total>0&&hit>=total*.65)return true;
        // A system live-activity pill may cover the centre of the status bar.
        // Require pink evidence on BOTH exposed sides; X/ring proof still follows.
        return pinkBand(p,w,h,.04,.30)&&pinkBand(p,w,h,.80,.97);
    }
    private static boolean pinkBand(int[] p,int w,int h,double left,double right) {
        int hit=0,total=0;
        for(int y=(int)(h*.012);y<h*.045;y+=Math.max(1,h/300))for(int x=(int)(w*left);x<w*right;x+=Math.max(1,w/80)) {
            int v=p[y*w+x];total++;if(pink(v,false)&&((v>>16)&255)>170)hit++;
        }
        return total>0&&hit>=total*.60;
    }
    private static double edge(int[] p,int w,int h,double x,double y,double nx,double ny,double scale) {
        double best=0;
        for(int k=-2;k<=2;k++) {
            double sx=x+nx*k*scale*.75,sy=y+ny*k*scale*.75;
            int ix=(int)Math.round(sx),iy=(int)Math.round(sy);
            if(ix<0||ix>=w||iy<0||iy>=h)continue;
            int bright=light(p[iy*w+ix]),dark=255;
            for(int sign=-1;sign<=1;sign+=2) {
                int ax=(int)Math.round(sx+nx*sign*scale*1.8),ay=(int)Math.round(sy+ny*sign*scale*1.8);
                if(ax>=0&&ax<w&&ay>=0&&ay<h)dark=Math.min(dark,light(p[ay*w+ax]));
            }
            if(bright>=145&&dark<=145)best=Math.max(best,bright-dark);
        }
        return best;
    }
    private static double closeScore(int[] p,int w,int h,double cx,double cy,double radius,boolean coarse) {
        double scale=Math.max(.8,w/575.0),sum=0;int ring=0;int[] quadrants=new int[4];
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12,nx=Math.cos(a),ny=Math.sin(a);
            double score=edge(p,w,h,cx+nx*radius,cy+ny*radius,nx,ny,scale);
            if(score>=45){ring++;quadrants[i/6]++;sum+=score;}
        }
        if(ring<(coarse?16:19))return -1;for(int n:quadrants)if(n<(coarse?3:4))return -1;
        int strokes=0;
        for(int arm=0;arm<4;arm++) {
            double a=Math.PI/4+arm*Math.PI/2,nx=Math.cos(a),ny=Math.sin(a);int covered=0;
            for(int j=1;j<=6;j++) {
                double r=radius*(.08+j*.09);
                double score=edge(p,w,h,cx+nx*r,cy+ny*r,-ny,nx,scale);
                if(score>=50){covered++;strokes++;sum+=score;}
            }
            if(covered<(coarse?3:4))return -1;
        }
        return sum/(ring+strokes);
    }
    private static double xDetail(int[] p,int w,int h,double cx,double cy,double radius) {
        double a=0,b=0,aa=0,bb=0,ab=0;int count=0;double unit=radius*.6/12,blur=radius*.1;
        for(int sy=0;sy<24;sy++)for(int sx=0;sx<24;sx++) {
            double dx=(sx-11.5)/12,dy=(sy-11.5)/12;if(dx*dx+dy*dy>.9*.9)continue;
            double x=cx+(sx-11.5)*unit,y=cy+(sy-11.5)*unit;double avg=0;
            for(int ky=-1;ky<=1;ky++)for(int kx=-1;kx<=1;kx++) {
                int px=(int)Math.round(x+kx*blur),py=(int)Math.round(y+ky*blur);
                if(px<0||px>=w||py<0||py>=h)return -1;avg+=light(p[py*w+px]);
            }
            double value=light(p[(int)Math.round(y)*w+(int)Math.round(x)])-avg/9,expected=X_DETAIL[sy*24+sx];
            count++;a+=value;b+=expected;aa+=value*value;bb+=expected*expected;ab+=value*expected;
        }
        double va=aa-a*a/count,vb=bb-b*b/count;
        return va/count<20||vb<=0?-1:(ab-a*b/count)/Math.sqrt(va*vb);
    }
    private static BiliVisual.Result contrastClose(int[] p,int w,int h) {
        double[] scores=new double[100];double[][] seeds=new double[100][3];int used=0;
        double step=Math.max(1,w/575.0*2);
        for(double radius=w*.032;radius<=w*.040;radius+=step)
            for(double y=h*.058+radius*.25;y<h*.13;y+=step)
                for(double x=radius;x<w*.125;x+=step) {
                    if(y-radius<h*.05)continue;
                    double score=closeScore(p,w,h,x,y,radius,true);if(score<55)continue;int at=used;
                    if(used==100){at=0;for(int i=1;i<100;i++)if(scores[i]<scores[at])at=i;if(score<=scores[at])continue;}
                    else used++;
                    scores[at]=score;seeds[at]=new double[]{x,y,radius};
                }
        BiliVisual.Result found=null;double best=0,refine=Math.max(.75,w/575.0);
        for(int i=0;i<used;i++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dr=-1;dr<=1;dr++) {
            double x=seeds[i][0]+dx*refine,y=seeds[i][1]+dy*refine,r=seeds[i][2]+dr*refine;
            double score=closeScore(p,w,h,x,y,r,false);if(score<60)continue;
            double detail=xDetail(p,w,h,x,y,r);if(detail<.52)continue;score=detail*200+score*.05;
            int cx=(int)Math.round(x),cy=(int)Math.round(y);
            if(found!=null&&(Math.abs(found.x-cx)>w*.025||Math.abs(found.y-cy)>h*.012))return new BiliVisual.Result(0,0,-1,"圆圈X位置不唯一");
            if(score>best){best=score;found=new BiliVisual.Result(cx,cy,(int)Math.round(r*2),"已核对哔咔包名，左上圆圈及四臂X的明暗边缘匹配；不依赖广告横幅位置");}
        }
        return found;
    }
    private static Result none(String why,boolean waiting){return new Result(new BiliVisual.Result(0,0,0,why),SplashActions.NONE,waiting);}
    private PicaVisual(){}
}
