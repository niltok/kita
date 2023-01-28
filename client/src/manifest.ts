import {ResolverManifest} from "@pixi/assets";

export const manifest: ResolverManifest = {
    bundles: [
        {
            name: "ui",
            assets: [
                {
                    name: "Source Han Serif CN VF",
                    srcs: "/ui/SourceHanSerifCN-VF.ttf.woff2"
                }
            ]
        },
        {
            name: "blocks",
            assets: [
                {
                    name: ["0", "unknown"],
                    srcs: "/blocks/stub.png"
                }
            ]
        }
    ]
}