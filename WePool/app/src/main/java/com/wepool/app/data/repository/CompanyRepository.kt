package com.wepool.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.company.Company
import kotlinx.coroutines.tasks.await

class CompanyRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createCompany(company: Company): String {
        val docRef = firestore.collection("companies").add(company).await()
        return docRef.id // מחזיר את ה-ID של החברה החדשה
    }

    suspend fun getCompany(companyId: String): Company? {
        val snapshot = firestore.collection("companies").document(companyId).get().await()
        return snapshot.toObject(Company::class.java)
    }

    suspend fun getAllCompanies(): List<Pair<String, Company>> {
        val snapshot = firestore.collection("companies").get().await()
        return snapshot.documents.mapNotNull {
            val company = it.toObject(Company::class.java)
            company?.let { c -> it.id to c }
        }
    }

    suspend fun updateCompanyName(companyId: String, newName: String) {
        firestore.collection("companies").document(companyId)
            .update("companyName", newName)
            .await()
    }
}