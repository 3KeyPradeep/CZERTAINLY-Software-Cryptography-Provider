package com.czertainly.cp.soft.util;

import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContent;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.czertainly.cp.soft.attribute.EcdsaKeyAttributes;
import com.czertainly.cp.soft.attribute.RsaKeyAttributes;
import com.czertainly.cp.soft.collection.DigestAlgorithm;
import com.czertainly.cp.soft.collection.RsaSignatureScheme;
import com.czertainly.cp.soft.dao.entity.KeyData;
import com.czertainly.cp.soft.exception.NotSupportedException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import java.security.*;
import java.util.List;

public class SignatureUtil {

    public static Signature prepareSignature(KeyData key, List<RequestAttributeDto> signatureAttributes) {
        String signatureAlgorithm;

        switch (key.getAlgorithm()) {
            case RSA -> {
                final RsaSignatureScheme scheme = RsaSignatureScheme.valueOf(
                        AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                                RsaKeyAttributes.ATTRIBUTE_DATA_RSA_SIG_SCHEME, signatureAttributes, StringAttributeContent.class)
                                .getReference()
                );
                final DigestAlgorithm digest = DigestAlgorithm.valueOf(
                        AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                                RsaKeyAttributes.ATTRIBUTE_DATA_SIG_DIGEST, signatureAttributes, StringAttributeContent.class)
                                .getReference()
                );

                signatureAlgorithm = digest.getProviderName() + "WITHRSA";
                if (scheme == RsaSignatureScheme.PSS) {
                    signatureAlgorithm += "ANDMGF1";
                }

                return getInstanceSignature(signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
            }
            case ECDSA -> {
                final DigestAlgorithm digest = DigestAlgorithm.valueOf(
                        AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                                EcdsaKeyAttributes.ATTRIBUTE_DATA_SIG_DIGEST, signatureAttributes, StringAttributeContent.class)
                                .getReference()
                );

                signatureAlgorithm = digest.getProviderName() + "WITHECDSA";

                return getInstanceSignature(signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
            }
            case FALCON -> {
                return getInstanceSignature("FALCON", BouncyCastlePQCProvider.PROVIDER_NAME);
                /*
                if (key.getLength() == 512) {
                    return getInstanceSignature("Falcon-512", BouncyCastlePQCProvider.PROVIDER_NAME);
                } else {
                    return getInstanceSignature("Falcon-1024", BouncyCastlePQCProvider.PROVIDER_NAME);
                }
                */
            }
            case DILITHIUM -> {
                return getInstanceSignature("DILITHIUM", BouncyCastlePQCProvider.PROVIDER_NAME);
            }
            case SPHINCSPLUS -> {
                return getInstanceSignature("SPHINCSPlus", BouncyCastlePQCProvider.PROVIDER_NAME);
            }
            default -> throw new NotSupportedException("Cryptographic algorithm not supported");
        }
    }

    public static Signature getInstanceSignature(String algorithm, String provider) {
        try {
            return Signature.getInstance(algorithm, provider);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid algorithm for signature", e);
        } catch (NoSuchProviderException e) {
            throw new IllegalStateException("Invalid provider for signature", e);
        }
    }

    public static void initSigning(Signature signature, KeyData key) {
        try {
            signature.initSign(KeyStoreUtil.getPrivateKey(key));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Invalid key '"+key.getName()+"'", e);
        }
    }

    public static void initVerification(Signature signature, KeyData key) {
        try {
            signature.initVerify(KeyStoreUtil.getCertificate(key));
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Invalid key '"+key.getName()+"'", e);
        }
    }

    public static byte[] signData(Signature signature, byte[] data) throws SignatureException {
        signature.update(data);
        return signature.sign();
    }

    public static boolean verifyData(Signature signature, byte[] data, byte[] sign) throws SignatureException {
        signature.update(data);
        return signature.verify(sign);
    }
}
